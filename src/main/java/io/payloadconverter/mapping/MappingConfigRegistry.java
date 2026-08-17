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

/**
 * Carrega, no startup da aplicacao, todos os arquivos YAML de configuracao de conversao
 * encontrados em {@code payload-converter.mappings-location} (por padrao,
 * {@code classpath:mappings/*.yml}) e os mantem em memoria, indexados por {@code id}.
 * <p>
 * Falha rapido: um YAML invalido (schema incorreto, id duplicado) impede a aplicacao de subir.
 */
@Component
public class MappingConfigRegistry {

    private static final Logger log = LoggerFactory.getLogger(MappingConfigRegistry.class);

    private final Map<String, MappingConfig> configuracoes;

    public MappingConfigRegistry(
            ResourcePatternResolver resolver,
            @Value("${payload-converter.mappings-location:classpath:mappings/*.yml}") String localizacao) {

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Map<String, MappingConfig> encontradas = new LinkedHashMap<>();

        try {
            Resource[] recursos = resolver.getResources(localizacao);
            for (Resource recurso : recursos) {
                MappingConfig config;
                try (InputStream in = recurso.getInputStream()) {
                    config = yamlMapper.readValue(in, MappingConfig.class);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Falha ao interpretar o arquivo de mapeamento '" + recurso.getFilename() + "': " + e.getMessage(), e);
                }
                MappingConfig anterior = encontradas.put(config.id(), config);
                if (anterior != null) {
                    throw new IllegalStateException(
                            "Configuracao de mapeamento duplicada para id='" + config.id()
                                    + "' (conflito no arquivo: " + recurso.getFilename() + ")");
                }
                log.info("Configuracao de mapeamento carregada: id='{}' ({} regra(s)) de {}",
                        config.id(), config.mappings().size(), recurso.getFilename());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao procurar configuracoes de mapeamento em '" + localizacao + "'", e);
        }

        this.configuracoes = Map.copyOf(encontradas);
        if (configuracoes.isEmpty()) {
            log.warn("Nenhuma configuracao de mapeamento encontrada em '{}'", localizacao);
        } else {
            log.info("{} fluxo(s) de conversao disponivel(is): {}", configuracoes.size(), configuracoes.keySet());
        }
    }

    public MappingConfig obter(String id) {
        MappingConfig config = configuracoes.get(id);
        if (config == null) {
            throw new NoSuchElementException("Fluxo de conversao desconhecido: '" + id + "'. Disponiveis: " + configuracoes.keySet());
        }
        return config;
    }

    public Set<String> listarIds() {
        return configuracoes.keySet();
    }

    public Collection<MappingConfig> listar() {
        return configuracoes.values();
    }
}
