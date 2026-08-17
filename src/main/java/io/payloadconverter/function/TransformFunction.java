package io.payloadconverter.function;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.engine.ConversionContext;

import java.util.Map;

/**
 * Contrato de uma funcao de transformacao referenciavel pelo YAML via {@code transform.function}.
 * <p>
 * Implementacoes built-in ficam em {@code io.payloadconverter.function.builtin}. Para adicionar
 * uma funcao customizada, basta declarar um {@code @Component} implementando esta interface -
 * ela e descoberta e registrada automaticamente pelo {@link FunctionRegistry}.
 */
public interface TransformFunction {

    /** Nome usado no YAML ({@code transform.function}). Deve ser unico em todo o classpath. */
    String nome();

    /**
     * @param valorOrigem valor lido em {@code source} (ja resolvido), ou {@code null} se a regra
     *                     nao tiver {@code source} - funcoes como {@code concat}/{@code constante}
     *                     ignoram este parametro e constroem o valor a partir de {@code args}.
     * @param args         argumentos nomeados definidos em {@code transform.args} no YAML.
     * @param ctx          contexto de conversao atual (permite resolver outros caminhos).
     * @return valor resultante, ou {@code null}/nó nulo se nao houver valor.
     */
    JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx);
}
