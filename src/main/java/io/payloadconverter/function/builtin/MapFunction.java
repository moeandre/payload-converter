package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import io.payloadconverter.util.JsonValues;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Funcao "map": tabela de de-para simples (enum de origem -&gt; valor de destino).
 * <pre>
 * transform:
 *   function: map
 *   args:
 *     valores:
 *       F: PESSOA_FISICA
 *       J: PESSOA_JURIDICA
 *     padrao: DESCONHECIDO   # opcional; sem ele, valores fora da tabela viram nulo
 * </pre>
 */
@Component
public class MapFunction implements TransformFunction {

    @Override
    public String nome() {
        return "map";
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        Object chave = JsonValues.paraValorJava(valorOrigem);
        Object tabela = args.get("valores");
        if (!(tabela instanceof Map<?, ?> mapa)) {
            throw new IllegalArgumentException("Funcao 'map' requer o argumento 'valores' (um mapa)");
        }
        Object valor = chave == null ? null : ((Map<Object, Object>) mapa).get(String.valueOf(chave));
        if (valor == null) {
            valor = args.get("padrao");
        }
        return JsonValues.paraJsonNode(valor, ctx.mapper());
    }
}
