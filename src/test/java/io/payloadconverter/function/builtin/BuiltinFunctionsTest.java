package io.payloadconverter.function.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.payloadconverter.engine.ConversionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinFunctionsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ConversionContext contextoVazio() throws Exception {
        return ConversionContext.raiz(MAPPER.readTree("{}"), MAPPER, "teste");
    }

    @Test
    void mapFunctionDeveTraduzirValorConhecido() throws Exception {
        JsonNode resultado = new MapFunction().aplicar(
                new TextNode("CPF"),
                Map.of("valores", Map.of("CPF", "PESSOA_FISICA", "CNPJ", "PESSOA_JURIDICA")),
                contextoVazio());
        assertThat(resultado.asText()).isEqualTo("PESSOA_FISICA");
    }

    @Test
    void mapFunctionDeveUsarPadraoQuandoDesconhecido() throws Exception {
        JsonNode resultado = new MapFunction().aplicar(
                new TextNode("X"),
                Map.of("valores", Map.of("CPF", "PESSOA_FISICA"), "padrao", "DESCONHECIDO"),
                contextoVazio());
        assertThat(resultado.asText()).isEqualTo("DESCONHECIDO");
    }

    @Test
    void concatFunctionDeveJuntarCaminhosComSeparador() throws Exception {
        ConversionContext ctx = ConversionContext.raiz(MAPPER.readTree("{\"a\":\"Ana\",\"b\":\"Souza\"}"), MAPPER, "teste");
        JsonNode resultado = new ConcatFunction().aplicar(
                null, Map.of("origens", List.of("a", "b"), "separador", " "), ctx);
        assertThat(resultado.asText()).isEqualTo("Ana Souza");
    }

    @Test
    void dataFormatoFunctionDeveConverterPadrao() throws Exception {
        JsonNode resultado = new DataFormatoFunction().aplicar(
                new TextNode("1990-05-20"),
                Map.of("origem", "yyyy-MM-dd", "destino", "dd/MM/yyyy"),
                contextoVazio());
        assertThat(resultado.asText()).isEqualTo("20/05/1990");
    }

    @Test
    void substringFunctionDeveRecortarComLimites() throws Exception {
        JsonNode resultado = new SubstringFunction().aplicar(
                new TextNode("12345678900"), Map.of("inicio", 0, "fim", 3), contextoVazio());
        assertThat(resultado.asText()).isEqualTo("123");
    }
}
