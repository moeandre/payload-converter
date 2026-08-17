package io.payloadconverter.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathResolverWriterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void deveResolverCaminhoAninhado() throws Exception {
        JsonNode node = MAPPER.readTree("{\"documento\":{\"numero\":\"123\",\"tipo\":\"CPF\"}}");
        assertThat(PathResolver.resolve(node, "documento.numero").asText()).isEqualTo("123");
    }

    @Test
    void deveResolverIndiceDeArray() throws Exception {
        JsonNode node = MAPPER.readTree("{\"itens\":[{\"sku\":\"A\"},{\"sku\":\"B\"}]}");
        assertThat(PathResolver.resolve(node, "itens[1].sku").asText()).isEqualTo("B");
    }

    @Test
    void deveRetornarNuloParaCaminhoAusente() throws Exception {
        JsonNode node = MAPPER.readTree("{\"a\":1}");
        assertThat(PathResolver.resolve(node, "b.c")).isNull();
    }

    @Test
    void deveEscreverCriandoEstruturaAninhada() {
        ObjectNode destino = MAPPER.createObjectNode();
        PathWriter.write(destino, "cliente.endereco.cidade", new TextNode("SP"), MAPPER);
        assertThat(destino.at("/cliente/endereco/cidade").asText()).isEqualTo("SP");
    }

    @Test
    void deveEscreverEmIndiceDeArrayCriandoOsAnteriores() {
        ObjectNode destino = MAPPER.createObjectNode();
        PathWriter.write(destino, "itens[1].sku", new TextNode("B"), MAPPER);
        assertThat(destino.at("/itens/1/sku").asText()).isEqualTo("B");
        assertThat(destino.at("/itens/0").isNull()).isTrue();
    }
}
