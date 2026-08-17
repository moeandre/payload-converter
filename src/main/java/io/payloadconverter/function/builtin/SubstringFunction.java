package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Funcao "substring": recorta o texto de origem.
 * <pre>
 * transform:
 *   function: substring
 *   args:
 *     inicio: 0
 *     fim: 3        # opcional; se ausente, vai ate o final da string
 * </pre>
 * Indices fora dos limites sao ajustados (clamp) em vez de lancar excecao.
 */
@Component
public class SubstringFunction implements TransformFunction {

    @Override
    public String nome() {
        return "substring";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        if (valorOrigem == null || valorOrigem.isNull() || valorOrigem.isMissingNode()) {
            return NullNode.getInstance();
        }
        String texto = valorOrigem.asText();
        int inicio = clamp(((Number) args.getOrDefault("inicio", 0)).intValue(), texto.length());
        int fim = args.containsKey("fim")
                ? clamp(((Number) args.get("fim")).intValue(), texto.length())
                : texto.length();
        if (fim < inicio) {
            fim = inicio;
        }
        return new TextNode(texto.substring(inicio, fim));
    }

    private static int clamp(int valor, int tamanho) {
        return Math.max(0, Math.min(valor, tamanho));
    }
}
