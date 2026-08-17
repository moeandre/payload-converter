package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Funcao "trim": remove espacos em branco do inicio/fim do texto de origem. */
@Component
public class TrimFunction implements TransformFunction {

    @Override
    public String nome() {
        return "trim";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        if (valorOrigem == null || valorOrigem.isNull() || valorOrigem.isMissingNode()) {
            return NullNode.getInstance();
        }
        return new TextNode(valorOrigem.asText().trim());
    }
}
