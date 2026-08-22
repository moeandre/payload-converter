package io.payloadconverter.mapping.model;

import java.net.URI;
import java.util.List;

/**
 * Raiz de um arquivo de configuracao de conversao (YAML).
 *
 * @param id            identificador do fluxo, usado para selecionar a configuracao
 *                      (ex: no path da API {@code POST /convert/{id}})
 * @param descricao     texto livre, apenas documentacional
 * @param mercado       tag opcional de mercado/produto (ex: "seguro-auto"), usada para
 *                      agrupar fluxos e, opcionalmente, resolver componentes especificos
 * @param schemaOrigem  caminho (classpath ou filesystem, resolvido como {@link org.springframework.core.io.Resource})
 *                      de um JSON Schema opcional para validar o payload de origem antes da conversao
 * @param schemaDestino caminho de um JSON Schema opcional para validar o payload resultante apos a conversao
 * @param destino       URL opcional do sistema de destino. Quando presente, apos converter o
 *                      payload a API encaminha a requisicao para essa URL usando o mesmo
 *                      verbo HTTP recebido pelo orquestrador, e devolve a resposta do destino
 *                      de forma transparente (mesmo status e corpo). Quando ausente, a API
 *                      apenas retorna o payload convertido, sem encaminhar nada.
 * @param mappings      lista ordenada de regras de mapeamento de-para
 */
public record MappingConfig(
        String id,
        String descricao,
        String mercado,
        String schemaOrigem,
        String schemaDestino,
        String destino,
        List<MappingRule> mappings
) {
    public MappingConfig {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Configuracao de mapeamento sem 'id'");
        }
        if (destino != null && !destino.isBlank()) {
            try {
                URI uri = URI.create(destino);
                if (uri.getScheme() == null || uri.getHost() == null) {
                    throw new IllegalArgumentException("URL incompleta (falta esquema e/ou host)");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Campo 'destino' invalido: '" + destino + "' - " + e.getMessage(), e);
            }
        }
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
    }
}
