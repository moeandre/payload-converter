package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

/**
 * Funcao "numeroFormato": formata um numero de origem usando um padrao {@link DecimalFormat}.
 * <pre>
 * transform:
 *   function: numeroFormato
 *   args:
 *     padrao: "#,##0.00"
 *     locale: "pt-BR"   # opcional, default pt-BR
 * </pre>
 */
@Component
public class NumeroFormatoFunction implements TransformFunction {

    @Override
    public String nome() {
        return "numeroFormato";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        if (valorOrigem == null || valorOrigem.isNull() || valorOrigem.isMissingNode()) {
            return NullNode.getInstance();
        }
        String padrao = String.valueOf(args.get("padrao"));
        if (padrao == null || padrao.isBlank()) {
            throw new IllegalArgumentException("Funcao 'numeroFormato' requer o argumento 'padrao'");
        }
        Locale locale = args.containsKey("locale")
                ? Locale.forLanguageTag(String.valueOf(args.get("locale")))
                : Locale.of("pt", "BR");

        double valor = valorOrigem.isNumber() ? valorOrigem.asDouble() : Double.parseDouble(valorOrigem.asText());
        DecimalFormat formatador = new DecimalFormat(padrao, DecimalFormatSymbols.getInstance(locale));
        return new TextNode(formatador.format(valor));
    }
}
