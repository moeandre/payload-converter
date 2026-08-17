package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/** Funcao "maiuscula": converte o texto de origem para maiusculas (Locale pt-BR). */
@Component
public class MaiusculaFunction implements TransformFunction {

    @Override
    public String nome() {
        return "maiuscula";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        if (valorOrigem == null || valorOrigem.isNull() || valorOrigem.isMissingNode()) {
            return NullNode.getInstance();
        }
        return new TextNode(valorOrigem.asText().toUpperCase(Locale.of("pt", "BR")));
    }
}
