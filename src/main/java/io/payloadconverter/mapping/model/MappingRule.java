package io.payloadconverter.mapping.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Uma regra individual de de-para.
 * <p>
 * O valor final e resolvido, nesta ordem de prioridade, pelo primeiro campo presente:
 * <ol>
 *     <li>{@code component} - delega o calculo a um {@link io.payloadconverter.component.MarketComponent}
 *     ("componente de mercado") registrado pelo nome informado;</li>
 *     <li>{@code transform} - aplica uma funcao (built-in ou customizada) sobre o valor
 *     lido de {@code source} (quando presente) e/ou sobre os proprios {@code args};</li>
 *     <li>{@code source} - copia direta do valor lido no payload de origem;</li>
 *     <li>{@code forEach} - itera um array de origem e, para cada elemento, aplica as
 *     {@code mappings} aninhadas, produzindo um array no destino.</li>
 * </ol>
 *
 * @param target      caminho (dot notation, com indices opcionais {@code [n]}) no payload de
 *                     destino onde o valor sera escrito. Dentro de uma regra {@code forEach},
 *                     o caminho e relativo ao objeto de cada elemento gerado.
 * @param source       caminho no payload de origem (ou, dentro de um {@code forEach}, no
 *                     escopo atual) de onde o valor e lido.
 * @param when         expressao condicional opcional (DSL propria). Quando presente e
 *                     avaliada como falsa, a regra inteira e ignorada.
 * @param defaultValue valor usado quando o valor resolvido for nulo/ausente.
 * @param required     quando {@code true} e o valor final (apos aplicar {@code defaultValue})
 *                     continuar nulo/ausente, a conversao falha com erro descritivo.
 * @param transform    especificacao de funcao de transformacao a aplicar.
 * @param component    especificacao de componente de mercado a aplicar.
 * @param forEach      caminho, no escopo atual, de um array de origem a iterar.
 * @param as           nome da variavel de escopo usada para referenciar o elemento atual
 *                     dentro de {@code mappings} aninhadas (default: {@code "item"}).
 * @param mappings     regras aninhadas, usadas apenas quando {@code forEach} esta presente.
 */
public record MappingRule(
        String target,
        String source,
        String when,
        @JsonProperty("default") Object defaultValue,
        boolean required,
        TransformSpec transform,
        ComponentSpec component,
        String forEach,
        String as,
        List<MappingRule> mappings
) {
    public MappingRule {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Regra de mapeamento sem 'target'");
        }
        as = (as == null || as.isBlank()) ? "item" : as;
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
    }
}
