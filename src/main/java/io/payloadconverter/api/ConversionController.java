package io.payloadconverter.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.api.dto.FluxoResumo;
import io.payloadconverter.encaminhamento.Encaminhador;
import io.payloadconverter.encaminhamento.RespostaEncaminhada;
import io.payloadconverter.engine.PayloadConverter;
import io.payloadconverter.mapping.MappingConfigRegistry;
import io.payloadconverter.mapping.model.MappingConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * API do orquestrador de conversao.
 * <p>
 * {@code GET /convert} - lista os fluxos de conversao disponiveis (um por arquivo YAML).
 * <p>
 * {@code POST/PUT/PATCH/DELETE/GET /convert/{id}} - converte o payload informado usando o
 * fluxo {@code id}. Se o fluxo declarar {@code destino} no YAML, o payload convertido e
 * encaminhado para essa URL usando o mesmo verbo HTTP recebido aqui, e a resposta do
 * destino e devolvida de forma transparente (mesmo status/corpo); caso contrario, o
 * endpoint apenas retorna o payload convertido.
 */
@RestController
@RequestMapping("/convert")
public class ConversionController {

    private static final Logger log = LoggerFactory.getLogger(ConversionController.class);

    private final MappingConfigRegistry configuracoes;
    private final PayloadConverter conversor;
    private final Encaminhador encaminhador;

    public ConversionController(MappingConfigRegistry configuracoes, PayloadConverter conversor, Encaminhador encaminhador) {
        this.configuracoes = configuracoes;
        this.conversor = conversor;
        this.encaminhador = encaminhador;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FluxoResumo> listarFluxos() {
        return configuracoes.listar().stream()
                .map(c -> new FluxoResumo(c.id(), c.descricao(), c.mercado(), c.mappings().size()))
                .toList();
    }

    @RequestMapping(value = "/{id}",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Object> converter(
            @PathVariable String id,
            @RequestBody(required = false) JsonNode payloadOrigem,
            HttpServletRequest request) {

        MappingConfig config = configuracoes.obter(id);
        log.debug("[{}] payload de origem: {}", id, payloadOrigem);

        JsonNode convertido = conversor.convert(payloadOrigem, config);

        if (config.destino() == null || config.destino().isBlank()) {
            return ResponseEntity.ok(convertido);
        }

        HttpMethod metodo = HttpMethod.valueOf(request.getMethod());
        log.debug("[{}] payload convertido, encaminhado via {} para '{}': {}", id, metodo, config.destino(), convertido);

        RespostaEncaminhada resposta = encaminhador.encaminhar(metodo, URI.create(config.destino()), convertido);

        log.debug("[{}] resposta do destino: status={} corpo={}", id, resposta.status(), comoTexto(resposta.corpo()));
        log.info("[{}] encaminhado via {} para '{}': status {}", id, metodo, config.destino(), resposta.status());

        return ResponseEntity.status(resposta.status())
                .contentType(resposta.contentType() != null ? resposta.contentType() : MediaType.APPLICATION_OCTET_STREAM)
                .body(resposta.corpo());
    }

    private static String comoTexto(byte[] corpo) {
        return corpo == null ? "" : new String(corpo, StandardCharsets.UTF_8);
    }
}
