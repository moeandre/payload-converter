package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.function.TransformFunction;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Map;

/**
 * Funcao "dataFormato": reformata uma data/data-hora textual de um padrao para outro.
 * <pre>
 * transform:
 *   function: dataFormato
 *   args:
 *     origem: "yyyy-MM-dd"
 *     destino: "dd/MM/yyyy"
 * </pre>
 */
@Component
public class DataFormatoFunction implements TransformFunction {

    @Override
    public String nome() {
        return "dataFormato";
    }

    @Override
    public JsonNode aplicar(JsonNode valorOrigem, Map<String, Object> args, ConversionContext ctx) {
        if (valorOrigem == null || valorOrigem.isNull() || valorOrigem.isMissingNode()) {
            return NullNode.getInstance();
        }
        String texto = valorOrigem.asText();
        if (texto.isBlank()) {
            return NullNode.getInstance();
        }
        String padraoOrigem = String.valueOf(args.get("origem"));
        String padraoDestino = String.valueOf(args.get("destino"));
        if (padraoOrigem == null || padraoDestino == null) {
            throw new IllegalArgumentException("Funcao 'dataFormato' requer os argumentos 'origem' e 'destino' (padroes de data)");
        }

        DateTimeFormatter formatadorOrigem = DateTimeFormatter.ofPattern(padraoOrigem);
        DateTimeFormatter formatadorDestino = DateTimeFormatter.ofPattern(padraoDestino);

        TemporalAccessor valor;
        try {
            valor = formatadorOrigem.parseBest(texto, LocalDateTime::from, LocalDate::from);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Nao foi possivel interpretar '" + texto + "' com o padrao '" + padraoOrigem + "'", e);
        }
        return new TextNode(formatadorDestino.format(valor));
    }
}
