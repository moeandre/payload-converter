package io.payloadconverter.encaminhamento;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

/**
 * Encaminha o payload ja convertido para o sistema de destino configurado no fluxo
 * ({@code MappingConfig.destino}), usando o mesmo verbo HTTP recebido pelo orquestrador,
 * e devolve a resposta bruta (status, content-type, corpo) para ser repassada de forma
 * transparente ao chamador original.
 */
@Component
public class Encaminhador {

    private final RestClient restClient;

    public Encaminhador(
            RestClient.Builder builder,
            @Value("${payload-converter.encaminhamento.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${payload-converter.encaminhamento.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = builder.requestFactory(factory).build();
    }

    public RespostaEncaminhada encaminhar(HttpMethod metodo, URI destino, JsonNode corpo) {
        try {
            RestClient.RequestBodySpec requisicao = restClient.method(metodo).uri(destino);
            if (corpo != null && !corpo.isMissingNode()) {
                requisicao = requisicao.contentType(MediaType.APPLICATION_JSON).body(corpo);
            }
            return requisicao.exchange((req, resp) -> new RespostaEncaminhada(
                    resp.getStatusCode().value(),
                    resp.getHeaders().getContentType(),
                    resp.getBody().readAllBytes()
            ), false);
        } catch (RestClientException e) {
            throw new DestinoIndisponivelException(destino.toString(), e);
        }
    }
}
