package io.payloadconverter.mapping.model;

import java.util.Map;

/**
 * Especificacao de um "componente de mercado" (plugin de regra de negocio) a aplicar.
 *
 * @param nome nome pelo qual o {@link io.payloadconverter.component.MarketComponent} foi
 *             registrado (ex: "seguroAuto.calculoPremio")
 * @param args argumentos nomeados repassados ao componente
 */
public record ComponentSpec(String nome, Map<String, Object> args) {
    public ComponentSpec {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Especificacao de 'component' sem 'nome'");
        }
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
