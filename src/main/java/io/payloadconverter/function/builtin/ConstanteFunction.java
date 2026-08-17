package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import io.payloadconverter.util.JsonValues;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Funcao "constante": ignora {@code source} e sempre retorna {@code args.valor}.
 * Util para preencher campos fixos exigidos pelo destino (ex: versao de contrato, flags fixas).
 * <pre>
 * transform:
 *   function: constante
 *   args:
 *     valor: "V2"
 * </pre>
 */
@Component
public class ConstanteFunction implements TransformFunction {

    @Override
    public String nome() {
        return "constante";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        return JsonValues.paraJsonNode(args.get("valor"), ctx.mapper());
    }
}
