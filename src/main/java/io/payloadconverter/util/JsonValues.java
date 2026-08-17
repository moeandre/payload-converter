package io.payloadconverter.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * Conversao entre {@link JsonNode} e valores Java "soltos" (String, Number, Boolean, null),
 * usada pela DSL de expressoes, pelas funcoes de transformacao e pelo motor de conversao.
 */
public final class JsonValues {

    private JsonValues() {
    }

    /** Converte um {@link JsonNode} escalar para o tipo Java correspondente. Arrays/objetos sao retornados como o proprio node. */
    public static Object paraValorJava(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        // arrays e objetos: devolve o proprio node (uso avancado, ex: componentes de mercado)
        return node;
    }

    /** Converte um valor Java arbitrario (ou um {@link JsonNode} ja pronto) para {@link JsonNode}. */
    public static JsonNode paraJsonNode(Object valor, ObjectMapper mapper) {
        if (valor == null) {
            return NullNode.getInstance();
        }
        if (valor instanceof JsonNode node) {
            return node;
        }
        return mapper.valueToTree(valor);
    }

    /** Checagem de "veracidade" usada por caminhos/literais usados diretamente como expressao booleana. */
    public static boolean ehVerdadeiro(Object valor) {
        if (valor == null) {
            return false;
        }
        if (valor instanceof Boolean b) {
            return b;
        }
        if (valor instanceof String s) {
            return !s.isEmpty();
        }
        if (valor instanceof Number n) {
            return n.doubleValue() != 0.0;
        }
        return true;
    }
}
