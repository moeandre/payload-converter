package io.payloadconverter.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.payloadconverter.component.ComponentRegistry;
import io.payloadconverter.encaminhamento.Encaminhador;
import io.payloadconverter.engine.MappingEngine;
import io.payloadconverter.function.FunctionRegistry;
import io.payloadconverter.mapping.MappingConfigRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o encaminhamento transparente ponta-a-ponta: um servidor HTTP real (embutido no
 * JDK) faz o papel do "sistema de destino"; o controller e montado manualmente (sem
 * contexto Spring) apontando para um fluxo cujo {@code destino} e esse servidor.
 */
class ConversionControllerForwardTest {

    private HttpServer servidorFalso;
    private Path diretorioTemp;

    @AfterEach
    void limpar() throws IOException {
        if (servidorFalso != null) {
            servidorFalso.stop(0);
        }
        if (diretorioTemp != null && Files.exists(diretorioTemp)) {
            try (var stream = Files.walk(diretorioTemp)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    void deveEncaminharUsandoOMesmoVerboEDevolverRespostaTransparente() throws Exception {
        AtomicReference<String> metodoRecebido = new AtomicReference<>();
        AtomicReference<String> corpoRecebido = new AtomicReference<>();

        servidorFalso = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidorFalso.createContext("/apolices", exchange -> {
            metodoRecebido.set(exchange.getRequestMethod());
            corpoRecebido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resposta = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, resposta.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resposta);
            }
        });
        servidorFalso.start();
        int porta = servidorFalso.getAddress().getPort();

        diretorioTemp = Files.createTempDirectory("forward-test");
        Files.writeString(diretorioTemp.resolve("fluxo.yml"), """
                id: fluxo-encaminhado
                destino: http://localhost:%d/apolices
                mappings:
                  - target: numeroCpf
                    source: documento.numero
                """.formatted(porta));

        MappingConfigRegistry registry = new MappingConfigRegistry(
                new PathMatchingResourcePatternResolver(),
                "file:" + diretorioTemp.toString().replace('\\', '/') + "/*.yml");

        ObjectMapper mapper = new ObjectMapper();
        MappingEngine engine = new MappingEngine(mapper, new FunctionRegistry(List.of()), new ComponentRegistry(List.of()));
        Encaminhador encaminhador = new Encaminhador(RestClient.builder(), 2000, 5000);

        ConversionController controller = new ConversionController(registry, engine, encaminhador);

        JsonNode payload = mapper.readTree("{\"documento\":{\"numero\":\"123\"}}");
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/convert/fluxo-encaminhado");

        ResponseEntity<Object> resposta = controller.converter("fluxo-encaminhado", payload, request);

        assertThat(metodoRecebido.get()).isEqualTo("PUT");
        assertThat(corpoRecebido.get()).isEqualTo("{\"numeroCpf\":\"123\"}");

        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(new String((byte[]) resposta.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"ok\":true}");
    }

    @Test
    void semDestinoConfiguradoDeveApenasRetornarOConvertido() throws Exception {
        diretorioTemp = Files.createTempDirectory("forward-test-sem-destino");
        Files.writeString(diretorioTemp.resolve("fluxo.yml"), """
                id: fluxo-sem-destino
                mappings:
                  - target: numeroCpf
                    source: documento.numero
                """);

        MappingConfigRegistry registry = new MappingConfigRegistry(
                new PathMatchingResourcePatternResolver(),
                "file:" + diretorioTemp.toString().replace('\\', '/') + "/*.yml");

        ObjectMapper mapper = new ObjectMapper();
        MappingEngine engine = new MappingEngine(mapper, new FunctionRegistry(List.of()), new ComponentRegistry(List.of()));
        Encaminhador encaminhador = new Encaminhador(RestClient.builder(), 2000, 5000);

        ConversionController controller = new ConversionController(registry, engine, encaminhador);

        JsonNode payload = mapper.readTree("{\"documento\":{\"numero\":\"123\"}}");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/convert/fluxo-sem-destino");

        ResponseEntity<Object> resposta = controller.converter("fluxo-sem-destino", payload, request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(((JsonNode) resposta.getBody()).at("/numeroCpf").asText()).isEqualTo("123");
    }
}
