package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import io.payloadconverter.util.JsonValues;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Funcao "concat": concatena varios caminhos de origem em uma unica string.
 * Ignora {@code source} da regra - a lista de origens vem em {@code args.origens}.
 * <pre>
 * transform:
 *   function: concat
 *   args:
 *     origens: [cliente.primeiroNome, cliente.sobrenome]
 *     separador: " "        # opcional, default ""
 *     ignorarNulos: true     # opcional, default true
 * </pre>
 */
@Component
public class ConcatFunction implements TransformFunction {

    @Override
    public String nome() {
        return "concat";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        Object origensArg = args.get("origens");
        if (!(origensArg instanceof List<?> origens)) {
            throw new IllegalArgumentException("Funcao 'concat' requer o argumento 'origens' (uma lista de caminhos)");
        }
        String separador = String.valueOf(args.getOrDefault("separador", ""));
        boolean ignorarNulos = !Boolean.FALSE.equals(args.get("ignorarNulos"));

        StringBuilder sb = new StringBuilder();
        boolean primeiro = true;
        for (Object origem : origens) {
            Object valor = JsonValues.paraValorJava(ctx.resolver(String.valueOf(origem)));
            if (valor == null) {
                if (ignorarNulos) {
                    continue;
                }
                valor = "";
            }
            if (!primeiro) {
                sb.append(separador);
            }
            sb.append(valor);
            primeiro = false;
        }
        return new TextNode(sb.toString());
    }
}
