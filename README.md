# payload-converter

Orquestrador de conversão de payloads (**de-para**) entre sistemas, configurável via
arquivos **YAML** — sem recompilar código para adicionar um novo fluxo de conversão.

Stack: **Java 21+**, **Spring Boot 3.5**, Jackson (JSON + YAML). API REST, sem estado,
com Virtual Threads habilitadas (Java 21) para alta concorrência.

> ✅ Compilado e testado localmente — 34/34 testes passando (`mvn clean test`,
> `BUILD SUCCESS`), incluindo o encaminhamento ponta-a-ponta contra um servidor HTTP real.

## Por que assim

- **Configurável**: cada fluxo (par origem→destino) é um arquivo YAML. Subir um novo
  de-para é criar um arquivo, não fazer deploy de código novo.
- **Aninhamento nos dois lados**: caminhos com dot-notation (`documento.numero`) e um
  operador `forEach` para transformar array de origem em array de destino, aninhando
  regras livremente.
- **Condicionais**: DSL de expressão própria (`when: "documento.tipo == 'CPF'"`) —
  deliberadamente **não** é um motor de script genérico (SpEL/JS): sem execução de
  código arbitrário, fácil de auditar, rápida de avaliar.
- **Componentes de mercado**: regras de negócio específicas de um produto/mercado
  (ex: cálculo de prêmio de seguro-auto) são plugáveis via interface Java
  (`MarketComponent`), referenciadas pelo nome no YAML — a lógica complexa continua em
  Java testável, só o *encaixe* dela no de-para é declarativo.
- **Validável**: JSON Schema opcional para o payload de origem e/ou de destino,
  declarado no próprio YAML do fluxo.
- **Configuração viva**: os YAML podem ser recarregados sem reiniciar a aplicação -
  automaticamente (watcher de filesystem) ou sob demanda (endpoint admin).
- **Encaminhamento transparente**: um fluxo pode declarar `destino` no YAML - a API
  converte e encaminha para essa URL usando o mesmo verbo HTTP recebido, devolvendo a
  resposta do sistema de destino tal como veio.
- **Rápido**: configuração é parseada uma vez e mantida em memória; expressões `when` e
  schemas são compilados/parseados uma única vez e cacheados; toda a travessia usa a
  árvore `JsonNode` do Jackson, sem reflection dinâmica.

## Como rodar

Precisa de JDK 21+. O projeto inclui o **Maven Wrapper** (`./mvnw` / `mvnw.cmd`), então
não precisa ter Maven instalado - mas se já tiver, `mvn` funciona igual:

```bash
./mvnw spring-boot:run
```

(Windows: `mvnw.cmd spring-boot:run`)

Rodar os testes:

```bash
./mvnw clean test
```

Empacotar e rodar o jar:

```bash
./mvnw clean package
java -jar target/payload-converter-0.1.0-SNAPSHOT.jar
```

Testar o fluxo de exemplo (`src/main/resources/mappings/sistemaA-para-sistemaB.yml`):

```bash
curl -X POST http://localhost:8080/convert/sistemaA-para-sistemaB \
  -H "Content-Type: application/json" \
  -d '{
        "documento": { "numero": "12345678900", "tipo": "CPF" },
        "cliente": { "primeiroNome": "Ana", "sobrenome": "Souza", "nascimento": "1990-05-20" },
        "veiculo": { "valorFipe": 45000.0 },
        "condutor": { "classeBonus": 5 },
        "produtos": [ { "codigo": "COB-CASCO", "qtd": 1 } ]
      }'
```

`GET /convert` lista os fluxos disponíveis. `GET /actuator/health` para healthcheck.

## Estrutura do projeto

```
src/main/java/io/payloadconverter/
  mapping/model/      # MappingConfig, MappingRule, TransformSpec, ComponentSpec (records)
  mapping/            # MappingConfigRegistry (carrega/recarrega os YAML) + MappingHotReloadWatcher
  engine/              # MappingEngine (motor), SchemaValidatingConverter, ConversionContext, PathResolver/Writer
  expression/          # DSL de condicionais: Lexer, ExpressionParser, ExpressionEvaluator
  function/            # TransformFunction + FunctionRegistry
  function/builtin/    # funções built-in: map, concat, dataFormato, substring, ...
  component/           # MarketComponent + ComponentRegistry ("componentes de mercado")
  component/exemplo/   # exemplo de componente de mercado (seguro-auto)
  schema/              # SchemaValidator + SchemaValidationException (JSON Schema)
  encaminhamento/      # Encaminhador (proxy transparente para o 'destino' do fluxo)
  api/                 # ConversionController, AdminController, GlobalExceptionHandler
src/main/resources/
  application.yml
  mappings/*.yml       # os fluxos de conversão
  schemas/*.json       # JSON Schemas usados pelos fluxos (opcional)
src/test/java/...       # testes unitários e de integração (MockMvc)
```

## Schema do YAML de mapeamento

```yaml
id: sistemaA-para-sistemaB        # obrigatório - usado em POST /convert/{id}
descricao: "texto livre"          # opcional
mercado: seguro-auto              # opcional, apenas documentacional/agrupamento
schemaOrigem: schemas/x.json      # opcional - JSON Schema do payload de origem
schemaDestino: schemas/y.json     # opcional - JSON Schema do payload de destino
destino: https://sistema-b/x     # opcional - encaminha o convertido pra cá, mesmo verbo recebido

mappings:                         # lista ordenada de regras
  - target: caminho.no.destino    # obrigatório
    source: caminho.na.origem     # de onde ler (copia direta se não houver transform/component)
    when: "expressão condicional" # opcional - regra é ignorada se avaliar falso
    default: valor                # opcional - usado se o valor resolvido for nulo/ausente
    required: true                # opcional (default false) - falha a conversão se ausente
    transform:                    # opcional - aplica uma função sobre o valor
      function: nomeDaFuncao
      args: { ... }
    component:                    # opcional - delega a um "componente de mercado"
      nome: mercado.regra
      args: { ... }
    forEach: caminho.do.array     # + mappings: para aninhar array origem -> array destino
    as: item                      # nome da variável do elemento atual (default "item")
    mappings: [ ... ]             # regras aninhadas, usadas só com forEach
```

Prioridade de resolução do valor de uma regra (a primeira que se aplicar):
**`component` → `transform` → `source` → `forEach`+`mappings`**.

### Exemplo: campo aninhado → achatado, com condicional

Resolve exatamente o caso citado no pedido original (`documento{numero,tipo}` →
`numeroCpf`/`numeroCnpj`):

```yaml
- target: numeroCpf
  source: documento.numero
  when: "documento.tipo == 'CPF'"

- target: numeroCnpj
  source: documento.numero
  when: "documento.tipo == 'CNPJ'"
```

### Exemplo: array aninhado (forEach)

```yaml
- target: itens
  forEach: produtos      # array no payload de origem
  as: item                # nome da variável (opcional, default "item")
  mappings:
    - target: sku
      source: item.codigo
    - target: numeroApoliceOrigem
      source: root.documento.numero   # 'root.' sempre acessa o payload de origem completo
```

Dentro de `mappings` aninhadas, caminhos não prefixados resolvem contra o elemento atual
(`item`, ou o nome dado em `as`); o prefixo `root.` sempre aponta para o payload de
origem original, não importa o nível de aninhamento. `forEach` pode ser aninhado dentro
de `forEach` (arrays de arrays), cada nível ganha seu próprio `as`.

### Exemplo: de-para (enum) e concatenação

```yaml
- target: cliente.tipoPessoa
  source: documento.tipo
  transform:
    function: map
    args:
      valores: { CPF: PESSOA_FISICA, CNPJ: PESSOA_JURIDICA }
      padrao: DESCONHECIDO     # opcional

- target: cliente.nomeCompleto
  transform:
    function: concat
    args:
      separador: " "
      origens: [cliente.primeiroNome, cliente.sobrenome]
```

### Exemplo: componente de mercado

```yaml
- target: premio.valorFinal
  component:
    nome: seguroAuto.calculoPremio
    args:
      aliquotaBase: 0.045
      descontoBonusPorClasse: 0.05
```

Veja o fluxo completo em
[src/main/resources/mappings/sistemaA-para-sistemaB.yml](src/main/resources/mappings/sistemaA-para-sistemaB.yml).

## DSL de condicionais (`when`)

Não é SpEL nem JavaScript — é uma gramática pequena e fechada, para evitar execução de
código arbitrário vindo de configuração:

| Categoria    | Sintaxe                                              |
|--------------|-------------------------------------------------------|
| Igualdade    | `campo == 'valor'`, `campo != 123`                    |
| Relacional   | `idade >= 18`, `idade < 65` (numérico)                |
| Lista        | `documento.tipo in ['CPF', 'CNPJ']`                   |
| Existência   | `exists(documento.numero)`                            |
| Lógicos      | `a && b`, `a \|\| b`, `!a`                            |
| Agrupamento  | `(a == 1 || a == 2) && b`                             |
| Literais     | strings `'x'`/`"x"`, números, `true`, `false`, `null` |
| Caminho puro | `when: "documento.numero"` → verdadeiro se presente/não-vazio |

Precedência (menor → maior): `\|\|`, `&&`, `!`, comparação/`in`. Nota: `!` nega a
expressão inteira à sua direita (ex.: `!a == b` é `!(a == b)`, não `(!a) == b`) — use
parênteses se quiser deixar isso explícito.

## Funções built-in (`transform.function`)

| Função           | args                                       | Descrição                                   |
|------------------|---------------------------------------------|----------------------------------------------|
| `map`            | `valores` (mapa), `padrao` (opcional)       | tabela de-para (enum origem → valor destino)  |
| `concat`         | `origens` (lista de caminhos), `separador`, `ignorarNulos` | concatena vários campos           |
| `coalesce`       | `origens` (lista de caminhos)               | primeiro valor não nulo (`source` + `origens`) |
| `constante`      | `valor`                                      | ignora `source`, retorna valor fixo           |
| `dataFormato`    | `origem`, `destino` (padrões `DateTimeFormatter`) | reformata data/data-hora                 |
| `maiuscula` / `minuscula` | -                                    | caixa alta/baixa                              |
| `trim`           | -                                             | remove espaços das pontas                     |
| `substring`      | `inicio`, `fim` (opcional)                  | recorta string (limites tolerantes)           |
| `numeroFormato`  | `padrao` (`DecimalFormat`), `locale` (opcional, default `pt-BR`) | formata número           |

### Estendendo com funções e componentes próprios

Funções (`io.payloadconverter.function.TransformFunction`) e componentes de mercado
(`io.payloadconverter.component.MarketComponent`) são descobertos automaticamente pelo
Spring - basta implementar a interface e anotar com `@Component`:

```java
@Component
public class MeuComponenteDeMercado implements MarketComponent {
    @Override public String nome() { return "credito.scoreDeRisco"; }
    @Override public JsonNode aplicar(Map<String, Object> args, ConversionContext ctx) {
        // leia o que precisar via ctx.resolver("qualquer.caminho")
        // e retorne um JsonNode (ex: DoubleNode, TextNode, BooleanNode)
    }
}
```

Nenhum outro cadastro é necessário — o `FunctionRegistry`/`ComponentRegistry` injeta
`List<TransformFunction>`/`List<MarketComponent>` e monta o índice por nome no startup,
falhando cedo (na subida da aplicação) se dois beans usarem o mesmo nome.

## Validação por JSON Schema

Cada fluxo pode declarar, opcionalmente, um schema para o payload de **origem** e/ou de
**destino** (draft 2020-12, via [`com.networknt:json-schema-validator`](https://github.com/networknt/json-schema-validator)):

```yaml
schemaOrigem: schemas/sistemaA.schema.json
schemaDestino: schemas/sistemaB.schema.json
```

- `schemaOrigem` é validado **antes** de qualquer regra de mapeamento rodar.
- `schemaDestino` é validado **depois** que o payload de destino foi montado.
- Falha de validação → `422` (`SchemaValidationException` estende `ConversionException`,
  então cai no mesmo handler/código de erro `falha_conversao`), com a lista de violações
  do schema concatenada na mensagem.
- Caminhos são resolvidos como `classpath:` por padrão (ou `file:`/`http:` explícitos);
  schemas compilados ficam em cache em memória.
- Ambos os campos são opcionais e independentes - um fluxo pode não ter nenhum, só
  origem, só destino, ou os dois. Veja
  [schemas/sistemaA.schema.json](src/main/resources/schemas/sistemaA.schema.json) e
  [schemas/sistemaB.schema.json](src/main/resources/schemas/sistemaB.schema.json).

## Hot-reload dos YAML (sem reiniciar)

Os arquivos de mapeamento podem ser recarregados em tempo de execução, de duas formas
complementares:

1. **Automático** (`MappingHotReloadWatcher`): observa o diretório configurado em
   `payload-converter.mappings-location` via `java.nio.file.WatchService` e recarrega
   sozinho quando um `.yml`/`.yaml` é criado, alterado ou removido (com debounce de
   `payload-converter.hot-reload.debounce-ms`, default 300ms, para agrupar rajadas de
   eventos). Só funciona quando a localização resolve para um **diretório real em
   disco** - rodando via `mvn`/IDE (`target/classes/mappings`), ou apontando para um
   diretório externo com `mappings-location: file:/etc/payload-converter/mappings/*.yml`
   em produção. Dentro de um JAR empacotado não há diretório real para observar, e o
   watcher fica inativo (log informativo na subida) - use a opção manual abaixo.
   Desativável com `payload-converter.hot-reload.enabled: false`.
2. **Manual** (sempre disponível, independe do watcher):
   ```bash
   curl -X POST http://localhost:8080/admin/mappings/reload
   ```
   Retorna os fluxos carregados após a recarga.

Em ambos os casos, **a recarga é atômica e resiliente**: se o novo conjunto de arquivos
tiver um YAML inválido ou um `id` duplicado, a recarga é abortada, o erro é reportado
(no endpoint) ou logado (no watcher automático), e a configuração anterior continua
válida e servindo requisições normalmente - nunca fica um estado quebrado no ar.

## Encaminhamento transparente (`destino`)

Além de só converter e devolver o resultado, um fluxo pode declarar `destino` no YAML:

```yaml
destino: https://sistema-b.example.com/apolices
```

Quando presente, `/convert/{id}` passa a aceitar **qualquer verbo** (`GET`, `POST`,
`PUT`, `PATCH`, `DELETE`) e, depois de converter o payload:

1. Encaminha o payload convertido para `destino`, usando **o mesmo verbo HTTP** com que
   o chamador chamou o orquestrador (chegou `PUT`? sai `PUT`; chegou `DELETE`? sai
   `DELETE`).
2. Devolve a resposta do sistema de destino **de forma transparente** ao chamador
   original - mesmo status HTTP, mesmo `Content-Type`, mesmo corpo (bytes), sem
   reinterpretar nem envelopar nada.

Quando `destino` **não** é declarado, o comportamento é o de sempre: o endpoint só
retorna o payload convertido (`200`), sem chamar ninguém.

```bash
curl -X PUT http://localhost:8080/convert/algum-fluxo-com-destino \
  -H "Content-Type: application/json" -d '{ ... payload de origem ... }'
# -> a resposta aqui e exatamente o que https://sistema-b.example.com/... respondeu ao PUT
```

Detalhes relevantes:

- **Logging**: os payloads (de origem, o convertido enviado ao destino, e a resposta
  recebida) só são logados em **DEBUG** (`io.payloadconverter: DEBUG` no `application.yml`
  ou via `-Dlogging.level.io.payloadconverter=DEBUG`) - em produção, com o nível padrão
  (`INFO`), nenhum conteúdo de payload vai pro log; só uma linha de auditoria com fluxo,
  verbo, URL de destino e status da resposta.
- **Falha ao encaminhar** (timeout, conexão recusada, DNS): `502 Bad Gateway`
  (`DestinoIndisponivelException`) - a resposta, nesse caso, não pode ser transparente
  porque não houve resposta nenhuma do outro lado.
- **Timeouts** configuráveis via `payload-converter.encaminhamento.connect-timeout-ms`
  (default 5000) e `read-timeout-ms` (default 15000).
- A URL é **fixa por fluxo, configurada por quem escreve o YAML** - não é informada pelo
  chamador da API. Isso é proposital: aceitar uma URL de destino arbitrária vinda do
  chamador abriria a aplicação como um proxy aberto (risco de SSRF contra rede interna);
  como fica só no YAML, quem controla o destino é quem tem acesso de escrita à
  configuração do orquestrador, não qualquer chamador da API.
- Ver [Encaminhador.java](src/main/java/io/payloadconverter/encaminhamento/Encaminhador.java)
  e o teste ponta-a-ponta em
  [ConversionControllerForwardTest.java](src/test/java/io/payloadconverter/api/ConversionControllerForwardTest.java)
  (sobe um servidor HTTP real do JDK fazendo de "Sistema B" para validar verbo + corpo + resposta espelhada).

## Tratamento de erros (API)

| Situação                                   | HTTP |
|---------------------------------------------|------|
| Fluxo (`id`) inexistente                     | 404  |
| Campo `required` ausente, função/componente desconhecido, array esperado e não encontrado, payload fora do JSON Schema, etc. | 422  |
| YAML de mapeamento inválido (ex: em `POST /admin/mappings/reload`) | 422  |
| JSON de entrada malformado                   | 400  |
| Sistema de destino (`destino`) inacessível   | 502  |
| Erro inesperado                              | 500  |
| *(qualquer outro caso)*                      | o mesmo status/corpo devolvido pelo `destino`, espelhado |

Corpo de erro padrão: `{ timestamp, status, erro, mensagem, fluxo, target }`.

## Próximos passos sugeridos (não implementados)

- Suporte a XML/outros formatos além de JSON, via adaptadores de (de)serialização plugáveis.
- Endpoint de "dry-run"/preview que mostra, por regra, qual valor foi resolvido - útil
  para depurar um YAML novo sem precisar ler logs.
- Watcher recursivo (hoje observa só o diretório configurado, não subdiretórios) e
  watch de múltiplos diretórios quando `mappings-location` aponta para mais de um.
- Métricas por fluxo (contagem/latência de conversões, taxa de erro) via Micrometer -
  o Actuator já está no classpath, faltaria expor `/actuator/metrics` com tags por `id`.
