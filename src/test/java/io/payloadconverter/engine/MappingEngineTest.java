package io.payloadconverter.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.payloadconverter.component.ComponentRegistry;
import io.payloadconverter.component.exemplo.CalculoPremioSeguroAutoComponent;
import io.payloadconverter.function.FunctionRegistry;
import io.payloadconverter.function.builtin.CoalesceFunction;
import io.payloadconverter.function.builtin.ConcatFunction;
import io.payloadconverter.function.builtin.ConstanteFunction;
import io.payloadconverter.function.builtin.DataFormatoFunction;
import io.payloadconverter.function.builtin.MaiusculaFunction;
import io.payloadconverter.function.builtin.MapFunction;
import io.payloadconverter.function.builtin.MinusculaFunction;
import io.payloadconverter.function.builtin.NumeroFormatoFunction;
import io.payloadconverter.function.builtin.SubstringFunction;
import io.payloadconverter.function.builtin.TrimFunction;
import io.payloadconverter.mapping.model.MappingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Teste de integracao do motor de conversao usando o fluxo de exemplo
 * ({@code mappings/sistemaA-para-sistemaB.yml}), cobrindo aninhamento, condicionais,
 * de-para, forEach e componente de mercado.
 */
class MappingEngineTest {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private MappingEngine engine;
    private MappingConfig config;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try (InputStream in = getClass().getResourceAsStream("/mappings/sistemaA-para-sistemaB.yml")) {
            config = yamlMapper.readValue(in, MappingConfig.class);
        }

        FunctionRegistry funcoes = new FunctionRegistry(List.of(
                new MapFunction(), new ConcatFunction(), new ConstanteFunction(), new CoalesceFunction(),
                new DataFormatoFunction(), new MaiusculaFunction(), new MinusculaFunction(),
                new TrimFunction(), new SubstringFunction(), new NumeroFormatoFunction()
        ));
        ComponentRegistry componentes = new ComponentRegistry(List.of(new CalculoPremioSeguroAutoComponent()));

        engine = new MappingEngine(jsonMapper, funcoes, componentes);
    }

    private JsonNode payloadExemplo() throws Exception {
        String json = """
                {
                  "documento": { "numero": "12345678900", "tipo": "CPF" },
                  "cliente": {
                    "primeiroNome": "Ana",
                    "sobrenome": "Souza",
                    "nascimento": "1990-05-20"
                  },
                  "veiculo": { "valorFipe": 45000.0 },
                  "condutor": { "classeBonus": 5 },
                  "produtos": [
                    { "codigo": "COB-CASCO", "qtd": 1 },
                    { "codigo": "COB-TERCEIROS", "qtd": 2 }
                  ]
                }
                """;
        return jsonMapper.readTree(json);
    }

    @Test
    void deveConverterPayloadCompleto() throws Exception {
        JsonNode destino = engine.convert(payloadExemplo(), config);

        assertThat(destino.at("/numeroCpf").asText()).isEqualTo("12345678900");
        assertThat(destino.has("numeroCnpj")).isFalse(); // condicional (when) nao satisfeita

        assertThat(destino.at("/cliente/tipoPessoa").asText()).isEqualTo("PESSOA_FISICA");
        assertThat(destino.at("/cliente/nomeCompleto").asText()).isEqualTo("Ana Souza");
        assertThat(destino.at("/cliente/dataNascimento").asText()).isEqualTo("20/05/1990");
        assertThat(destino.at("/cliente/documentoOrigem").asText()).isEqualTo("12345678900");
        assertThat(destino.at("/cliente/canalVenda").asText()).isEqualTo("DIGITAL"); // default (campo ausente na origem)

        assertThat(destino.at("/itens").isArray()).isTrue();
        assertThat(destino.at("/itens")).hasSize(2);
        assertThat(destino.at("/itens/0/sku").asText()).isEqualTo("COB-CASCO");
        assertThat(destino.at("/itens/0/quantidade").asInt()).isEqualTo(1);
        assertThat(destino.at("/itens/0/numeroApoliceOrigem").asText()).isEqualTo("12345678900");
        assertThat(destino.at("/itens/1/sku").asText()).isEqualTo("COB-TERCEIROS");
        assertThat(destino.at("/itens/1/quantidade").asInt()).isEqualTo(2);

        // 45000 * 0.045 * (1 - 0.05*5) = 1518.75
        assertThat(destino.at("/premio/valorFinal").asDouble()).isEqualTo(1518.75, offset(0.001));
    }

    @Test
    void deveFalharQuandoCampoObrigatorioAusente() throws Exception {
        JsonNode payload = jsonMapper.readTree("{\"documento\":{\"tipo\":\"CPF\"}}"); // sem 'numero'
        assertThrows(ConversionException.class, () -> engine.convert(payload, config));
    }
}
