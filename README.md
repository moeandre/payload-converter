# payload-converter

Orquestrador de conversão de payloads (**de-para**) entre sistemas, configurável via
arquivos **YAML** — sem recompilar código para adicionar um novo fluxo de conversão.

Stack: **Java 21+**, **Spring Boot 3.5**, Jackson (JSON + YAML). API REST, sem estado,
com Virtual Threads habilitadas (Java 21) para alta concorrência.

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
- **Rápido**: configuração é parseada uma vez (no startup) e mantida em memória;
  expressões `when` são compiladas (parseadas) uma única vez e cacheadas; toda a
  travessia usa a árvore `JsonNode` do Jackson, sem reflection dinâmica.

> ⚠️ Este ambiente de geração não tinha JDK/Maven instalados, então o projeto **não foi
> compilado nem testado aqui**. Rode `mvn test` localmente antes de confiar no código —
> veja [Como rodar](#como-rodar). Se algo não compilar, me diga o erro e eu conserto.

## Como rodar

```bash
mvn spring-boot:run
```

Ou empacotar e rodar o jar:

```bash
mvn clean package
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
  mapping/            # MappingConfigRegistry - carrega os YAML no startup
  engine/              # MappingEngine (motor), ConversionContext, PathResolver/Writer
  expression/          # DSL de condicionais: Lexer, ExpressionParser, ExpressionEvaluator
  function/            # TransformFunction + FunctionRegistry
  function/builtin/    # funções built-in: map, concat, dataFormato, substring, ...
  component/           # MarketComponent + ComponentRegistry ("componentes de mercado")
  component/exemplo/   # exemplo de componente de mercado (seguro-auto)
  api/                 # ConversionController + GlobalExceptionHandler
src/main/resources/
  application.yml
  mappings/*.yml       # os fluxos de conversão
src/test/java/...       # testes unitários e de integração (MockMvc)
```

## Schema do YAML de mapeamento

```yaml
id: sistemaA-para-sistemaB        # obrigatório - usado em POST /convert/{id}
descricao: "texto livre"          # opcional
mercado: seguro-auto              # opcional, apenas documentacional/agrupamento

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

## Tratamento de erros (API)

| Situação                                   | HTTP |
|---------------------------------------------|------|
| Fluxo (`id`) inexistente                     | 404  |
| Campo `required` ausente, função/componente desconhecido, array esperado e não encontrado, etc. | 422  |
| JSON de entrada malformado                   | 400  |
| Erro inesperado                              | 500  |

Corpo de erro padrão: `{ timestamp, status, erro, mensagem, fluxo, target }`.

## Próximos passos sugeridos (não implementados)

- Suporte a XML/outros formatos além de JSON, via adaptadores de (de)serialização plugáveis.
- Validação de schema (JSON Schema) do payload de origem/destino antes/depois da conversão.
- Hot-reload dos YAML sem reiniciar (hoje o carregamento é só no startup).
- Endpoint de "dry-run"/preview que mostra, por regra, qual valor foi resolvido - útil
  para depurar um YAML novo sem precisar ler logs.
