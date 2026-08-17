package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/** Funcao "minuscula": converte o texto de origem para minusculas (Locale pt-BR). */
@Component
public class MinusculaFunction implements TransformFunction {

    @Override
    public String nome() {
        return "minuscula";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        if (valorOrigem == null || valorOrigem.isNull() || valorOrigem.isMissingNode()) {
            return NullNode.getInstance();
        }
        return new TextNode(valorOrigem.asText().toLowerCase(Locale.of("pt", "BR")));
    }
}
