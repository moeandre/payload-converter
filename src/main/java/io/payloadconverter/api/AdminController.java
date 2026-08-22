package io.payloadconverter.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.mapping.MappingConfigRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Operacoes administrativas do orquestrador.
 * <p>
 * {@code POST /admin/mappings/reload} - forca uma releitura imediata dos YAML de
 * mapeamento, sem reiniciar a aplicacao (complementa o hot-reload automatico de
 * {@link io.payloadconverter.mapping.MappingHotReloadWatcher}, que so funciona quando os
 * arquivos estao em um diretorio real em disco).
 */
@RestController
@RequestMapping("/admin/mappings")
public class AdminController {

    private final MappingConfigRegistry configuracoes;
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    public AdminController(MappingConfigRegistry configuracoes) {
        this.configuracoes = configuracoes;
    }

    @PostMapping(value = "/reload", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> recarregar() {
        configuracoes.recarregar();
        return Map.of(
                "recarregado", true,
                "timestamp", Instant.now().toString(),
                "fluxos", configuracoes.listarIds()
        );
    }

    @RequestMapping(value = "/convert-test",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Object> converter(
            @RequestBody(required = false) JsonNode payloadOrigem) {

        log.info("[{}] payload recebido", payloadOrigem);

        return ResponseEntity.ok(payloadOrigem);
    }
}
