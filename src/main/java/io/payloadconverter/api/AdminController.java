package io.payloadconverter.api;

import io.payloadconverter.mapping.MappingConfigRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
