package io.payloadconverter.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.payloadconverter.mapping.model.MappingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Carrega os arquivos YAML de configuracao de conversao encontrados em
 * {@code payload-converter.mappings-location} (por padrao, {@code classpath:mappings/*.yml})
 * e os mantem em memoria, indexados por {@code id}.
 * <p>
 * Falha rapido na subida da aplicacao: um YAML invalido no startup impede a aplicacao de
 * subir. Depois de no ar, {@link #recarregar()} pode ser chamado a qualquer momento (via
 * {@code POST /admin/mappings/reload} ou pelo watcher de filesystem em
 * {@link MappingHotReloadWatcher}) para reler os arquivos - se a releitura falhar, a
 * configuracao anterior (ainda valida) continua servindo requisicoes normalmente.
 */
@Component
public class MappingConfigRegistry {

    private static final Logger log = LoggerFactory.getLogger(MappingConfigRegistry.class);

    private final ResourcePatternResolver resolver;
    private final String localizacao;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final AtomicReference<Map<String, MappingConfig>> configuracoes;

    public MappingConfigRegistry(
            ResourcePatternResolver resolver,
            @Value("${payload-converter.mappings-location:classpath:mappings/*.yml}") String localizacao) {
        this.resolver = resolver;
        this.localizacao = localizacao;
        this.configuracoes = new AtomicReference<>(carregarTudo());
        log.info("{} fluxo(s) de conversao disponivel(is): {}", configuracoes.get().size(), configuracoes.get().keySet());
    }

    /**
     * Re-percorre {@code payload-converter.mappings-location} e substitui, de forma atomica,
     * a configuracao em memoria. Se a releitura falhar (YAML invalido, id duplicado, etc.), a
     * configuracao anterior permanece ativa e a excecao e relancada para quem chamou.
     */
    public void recarregar() {
        Map<String, MappingConfig> novas = carregarTudo();
        Map<String, MappingConfig> antigas = configuracoes.getAndSet(novas);
        log.info("Configuracoes de mapeamento recarregadas: {} fluxo(s) agora disponivel(is): {} (antes: {})",
                novas.size(), novas.keySet(), antigas.keySet());
    }

    private Map<String, MappingConfig> carregarTudo() {
        Map<String, MappingConfig> encontradas = new LinkedHashMap<>();
        try {
            Resource[] recursos = resolver.getResources(localizacao);
            for (Resource recurso : recursos) {
                MappingConfig config;
                try (InputStream in = recurso.getInputStream()) {
                    config = yamlMapper.readValue(in, MappingConfig.class);
                } catch (Exception e) {
                    throw new MappingConfigException(
                            "Falha ao interpretar o arquivo de mapeamento '" + recurso.getFilename() + "': " + e.getMessage(), e);
                }
                MappingConfig anterior = encontradas.put(config.id(), config);
                if (anterior != null) {
                    throw new MappingConfigException(
                            "Configuracao de mapeamento duplicada para id='" + config.id()
                                    + "' (conflito no arquivo: " + recurso.getFilename() + ")");
                }
                log.info("Configuracao de mapeamento carregada: id='{}' ({} regra(s)) de {}",
                        config.id(), config.mappings().size(), recurso.getFilename());
            }
        } catch (IOException e) {
            throw new MappingConfigException("Falha ao procurar configuracoes de mapeamento em '" + localizacao + "'", e);
        }
        if (encontradas.isEmpty()) {
            log.warn("Nenhuma configuracao de mapeamento encontrada em '{}'", localizacao);
        }
        return Map.copyOf(encontradas);
    }

    public MappingConfig obter(String id) {
        MappingConfig config = configuracoes.get().get(id);
        if (config == null) {
            throw new NoSuchElementException("Fluxo de conversao desconhecido: '" + id + "'. Disponiveis: " + listarIds());
        }
        return config;
    }

    public Set<String> listarIds() {
        return configuracoes.get().keySet();
    }

    public Collection<MappingConfig> listar() {
        return configuracoes.get().values();
    }

    /** Localizacao (resource pattern) configurada - usado pelo watcher para descobrir quais diretorios observar. */
    String localizacao() {
        return localizacao;
    }

    ResourcePatternResolver resolver() {
        return resolver;
    }
}
