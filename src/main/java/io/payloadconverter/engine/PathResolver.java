package io.payloadconverter.engine;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leitura de valores em uma arvore {@link JsonNode} usando um mini caminho no estilo
 * "dot notation" com indices opcionais de array, por exemplo:
 * <pre>
 *   documento.numero
 *   enderecos[0].cidade
 *   produtos[2].precos[0]
 * </pre>
 * Qualquer segmento ausente (objeto, indice fora dos limites, ou node nulo) resulta em
 * {@code null}, nunca lanca excecao - a decisao do que fazer com um valor ausente
 * (usar default, exigir, ignorar) fica a cargo do motor de conversao.
 */
public final class PathResolver {

    // um segmento e um nome (qualquer caractere exceto '.' '[' ']') ou um indice "[n]"
    private static final Pattern SEGMENTO = Pattern.compile("([^.\\[\\]]+)|\\[(\\d+)]");

    private PathResolver() {
    }

    /** Quebra um caminho em segmentos: {@code String} para campo, {@code Integer} para indice. */
    public static List<Object> parse(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Caminho vazio ou invalido");
        }
        List<Object> segmentos = new ArrayList<>();
        Matcher m = SEGMENTO.matcher(path);
        while (m.find()) {
            if (m.group(1) != null) {
                segmentos.add(m.group(1));
            } else {
                segmentos.add(Integer.parseInt(m.group(2)));
            }
        }
        if (segmentos.isEmpty()) {
            throw new IllegalArgumentException("Caminho vazio ou invalido: '" + path + "'");
        }
        return segmentos;
    }

    /** Resolve o caminho a partir de {@code node}; retorna {@code null} se qualquer trecho estiver ausente. */
    public static JsonNode resolve(JsonNode node, String path) {
        JsonNode current = node;
        for (Object segmento : parse(path)) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }
            if (segmento instanceof String nome) {
                if (!current.isObject()) {
                    return null;
                }
                current = current.get(nome);
            } else {
                int indice = (Integer) segmento;
                if (!current.isArray() || indice < 0 || indice >= current.size()) {
                    return null;
                }
                current = current.get(indice);
            }
        }
        return (current == null || current.isMissingNode()) ? null : current;
    }
}
