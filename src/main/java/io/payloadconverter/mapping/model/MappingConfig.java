package io.payloadconverter.mapping.model;

import java.util.List;

/**
 * Raiz de um arquivo de configuracao de conversao (YAML).
 *
 * @param id        identificador do fluxo, usado para selecionar a configuracao
 *                  (ex: no path da API {@code POST /convert/{id}})
 * @param descricao texto livre, apenas documentacional
 * @param mercado   tag opcional de mercado/produto (ex: "seguro-auto"), usada para
 *                  agrupar fluxos e, opcionalmente, resolver componentes especificos
 * @param mappings  lista ordenada de regras de mapeamento de-para
 */
public record MappingConfig(
        String id,
        String descricao,
        String mercado,
        List<MappingRule> mappings
) {
    public MappingConfig {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Configuracao de mapeamento sem 'id'");
        }
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
    }
}
