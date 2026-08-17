package io.payloadconverter.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Contexto de execucao de uma conversao: o payload de origem completo, o "escopo atual"
 * (o proprio payload no nivel raiz, ou o elemento corrente dentro de um {@code forEach}),
 * e as variaveis de escopo nomeadas acumuladas ao longo do aninhamento.
 * <p>
 * Caminhos de origem ({@code source}, {@code forEach}, argumentos de funcoes/componentes)
 * sao resolvidos, nesta ordem:
 * <ol>
 *     <li>se o caminho comeca com {@code "<nomeDeEscopo>."} (ex: {@code "root."}, ou o nome
 *     definido em {@code as} de um {@code forEach} pai, default {@code "item"}), resolve a
 *     partir daquele escopo;</li>
 *     <li>caso contrario, resolve a partir do escopo atual.</li>
 * </ol>
 * {@code "root"} esta sempre disponivel e aponta para o payload de origem original,
 * independente do nivel de aninhamento.
 */
public final class ConversionContext {

    private static final String ESCOPO_RAIZ = "root";

    private final JsonNode atual;
    private final Map<String, JsonNode> escopos;
    private final ObjectMapper mapper;
    private final String flowId;

    private ConversionContext(JsonNode atual, Map<String, JsonNode> escopos, ObjectMapper mapper, String flowId) {
        this.atual = atual;
        this.escopos = escopos;
        this.mapper = mapper;
        this.flowId = flowId;
    }

    public static ConversionContext raiz(JsonNode payloadOrigem, ObjectMapper mapper, String flowId) {
        Map<String, JsonNode> escopos = new HashMap<>();
        escopos.put(ESCOPO_RAIZ, payloadOrigem);
        return new ConversionContext(payloadOrigem, escopos, mapper, flowId);
    }

    /** Cria um novo contexto para o elemento atual de um {@code forEach}, preservando os escopos externos. */
    public ConversionContext comEscopo(String nomeVariavel, JsonNode elemento) {
        Map<String, JsonNode> novosEscopos = new HashMap<>(escopos);
        novosEscopos.put(nomeVariavel, elemento);
        return new ConversionContext(elemento, novosEscopos, mapper, flowId);
    }

    public JsonNode atual() {
        return atual;
    }

    public JsonNode root() {
        return escopos.get(ESCOPO_RAIZ);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public String flowId() {
        return flowId;
    }

    /** Resolve um caminho de origem contra este contexto. Nunca lanca excecao por ausencia; retorna {@code null}. */
    public JsonNode resolver(String path) {
        int ponto = path.indexOf('.');
        String possivelEscopo = ponto > 0 ? path.substring(0, ponto) : path;
        JsonNode base = escopos.get(possivelEscopo);
        if (base != null) {
            if (ponto > 0) {
                return PathResolver.resolve(base, path.substring(ponto + 1));
            }
            return base; // o caminho e exatamente o nome do escopo
        }
        return PathResolver.resolve(atual, path);
    }
}
