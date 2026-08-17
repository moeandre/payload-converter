package io.payloadconverter.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.api.dto.FluxoResumo;
import io.payloadconverter.engine.PayloadConverter;
import io.payloadconverter.mapping.MappingConfigRegistry;
import io.payloadconverter.mapping.model.MappingConfig;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API do orquestrador de conversao.
 * <p>
 * {@code GET  /convert}      - lista os fluxos de conversao disponiveis (um por arquivo YAML).
 * {@code POST /convert/{id}} - converte o payload informado usando o fluxo {@code id}.
 */
@RestController
@RequestMapping("/convert")
public class ConversionController {

    private final MappingConfigRegistry configuracoes;
    private final PayloadConverter conversor;

    public ConversionController(MappingConfigRegistry configuracoes, PayloadConverter conversor) {
        this.configuracoes = configuracoes;
        this.conversor = conversor;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FluxoResumo> listarFluxos() {
        return configuracoes.listar().stream()
                .map(c -> new FluxoResumo(c.id(), c.descricao(), c.mercado(), c.mappings().size()))
                .toList();
    }

    @PostMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode converter(@PathVariable String id, @RequestBody JsonNode payloadOrigem) {
        MappingConfig config = configuracoes.obter(id);
        return conversor.convert(payloadOrigem, config);
    }
}
