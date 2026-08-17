package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Funcao "coalesce": retorna o primeiro valor nao nulo entre {@code source} (se presente)
 * seguido dos caminhos em {@code args.origens}.
 * <pre>
 * transform:
 *   function: coalesce
 *   args:
 *     origens: [contato.celular, contato.telefoneFixo]
 * </pre>
 */
@Component
public class CoalesceFunction implements TransformFunction {

    @Override
    public String nome() {
        return "coalesce";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        if (valorOrigem != null && !valorOrigem.isNull() && !valorOrigem.isMissingNode()) {
            return valorOrigem;
        }
        Object origensArg = args.get("origens");
        if (origensArg instanceof List<?> origens) {
            for (Object origem : origens) {
                JsonNode valor = ctx.resolver(String.valueOf(origem));
                if (valor != null && !valor.isNull() && !valor.isMissingNode()) {
                    return valor;
                }
            }
        }
        return null;
    }
}
