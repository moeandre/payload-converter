package io.payloadconverter.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Escrita de valores em uma arvore {@link ObjectNode}, criando objetos e arrays
 * intermediarios conforme necessario para o caminho de destino informado.
 */
public final class PathWriter {

    private PathWriter() {
    }

    public static void write(ObjectNode root, String path, JsonNode value, ObjectMapper mapper) {
        List<Object> segmentos = PathResolver.parse(path);
        JsonNode atual = root;
        for (int i = 0; i < segmentos.size(); i++) {
            Object segmento = segmentos.get(i);
            boolean ultimo = i == segmentos.size() - 1;
            Object proximo = ultimo ? null : segmentos.get(i + 1);

            if (segmento instanceof String nome) {
                ObjectNode objeto = comoObjeto(atual, path);
                if (ultimo) {
                    objeto.set(nome, value);
                } else {
                    atual = filhoOuNovo(objeto.get(nome), proximo, mapper, filho -> objeto.set(nome, filho));
                }
            } else {
                int indice = (Integer) segmento;
                ArrayNode array = comoArray(atual, path);
                crescerAte(array, indice);
                if (ultimo) {
                    array.set(indice, value);
                } else {
                    atual = filhoOuNovo(array.get(indice), proximo, mapper, filho -> array.set(indice, filho));
                }
            }
        }
    }

    private static JsonNode filhoOuNovo(JsonNode filhoAtual, Object proximoSegmento, ObjectMapper mapper,
                                         java.util.function.Consumer<JsonNode> setter) {
        if (filhoAtual != null && !filhoAtual.isNull() && !filhoAtual.isMissingNode()) {
            return filhoAtual;
        }
        JsonNode novo = (proximoSegmento instanceof Integer) ? mapper.createArrayNode() : mapper.createObjectNode();
        setter.accept(novo);
        return novo;
    }

    private static void crescerAte(ArrayNode array, int indice) {
        while (array.size() <= indice) {
            array.addNull();
        }
    }

    private static ObjectNode comoObjeto(JsonNode node, String path) {
        if (!(node instanceof ObjectNode objeto)) {
            throw new IllegalStateException(
                    "Esperava um objeto ao escrever em '" + path + "', mas encontrou "
                            + (node == null ? "null" : node.getNodeType()));
        }
        return objeto;
    }

    private static ArrayNode comoArray(JsonNode node, String path) {
        if (!(node instanceof ArrayNode array)) {
            throw new IllegalStateException(
                    "Esperava um array ao escrever em '" + path + "', mas encontrou "
                            + (node == null ? "null" : node.getNodeType()));
        }
        return array;
    }
}
