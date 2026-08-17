package io.payloadconverter.component;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.engine.ConversionContext;

import java.util.Map;

/**
 * "Componente de mercado": bloco de regra de negocio especifico de um mercado/produto
 * (ex: calculo de premio de um seguro-auto, regra de enquadramento de um produto de
 * credito) referenciavel pelo YAML via {@code component.nome}.
 * <p>
 * Diferente de uma {@link io.payloadconverter.function.TransformFunction}, que transforma
 * um unico valor, um componente tem acesso ao {@link ConversionContext} completo e decide
 * sozinho quais caminhos ler - util quando a regra de negocio combina varios campos e/ou
 * logica que nao vale a pena expressar em YAML.
 * <p>
 * Para adicionar um novo componente, basta declarar um {@code @Component} do Spring
 * implementando esta interface - ele e descoberto automaticamente pelo {@link ComponentRegistry}.
 */
public interface MarketComponent {

    /** Nome usado no YAML ({@code component.nome}), por convencao {@code "<mercado>.<regra>"}. */
    String nome();

    /**
     * @param args argumentos nomeados definidos em {@code component.args} no YAML.
     * @param ctx  contexto de conversao atual.
     * @return valor resultante, ou {@code null}/no nulo se nao houver valor.
     */
    JsonNode aplicar(Map<String, Object> args, ConversionContext ctx);
}
