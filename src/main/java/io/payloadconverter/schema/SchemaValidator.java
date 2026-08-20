package io.payloadconverter.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Valida payloads JSON contra arquivos de JSON Schema (draft 2020-12), referenciados pelo
 * YAML de mapeamento via {@code schemaOrigem}/{@code schemaDestino}.
 * <p>
 * Schemas compilados sao cacheados por caminho - o custo de parsing do schema so e pago
 * uma vez, mesmo sob alta concorrencia.
 */
@Component
public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    private final ResourceLoader resourceLoader;
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private final ConcurrentMap<String, JsonSchema> cache = new ConcurrentHashMap<>();

    public SchemaValidator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * @param rotulo usado apenas para compor mensagens de erro/campo (ex: "origem", "destino")
     * @throws SchemaValidationException se o payload nao atender ao schema
     */
    public void validar(JsonNode payload, String caminhoSchema, String flowId, String rotulo) {
        JsonSchema schema = cache.computeIfAbsent(caminhoSchema, this::carregar);
        Set<ValidationMessage> erros = schema.validate(payload);
        if (!erros.isEmpty()) {
            String mensagens = erros.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("; "));
            throw new SchemaValidationException(flowId, rotulo,
                    "Payload de " + rotulo + " nao atende ao schema '" + caminhoSchema + "': " + mensagens);
        }
    }

    private JsonSchema carregar(String caminho) {
        Resource resource = resourceLoader.getResource(normalizar(caminho));
        if (!resource.exists()) {
            throw new IllegalStateException("Schema JSON nao encontrado: '" + caminho + "'");
        }
        try (InputStream in = resource.getInputStream()) {
            JsonSchema schema = factory.getSchema(in);
            log.info("Schema JSON carregado: {}", caminho);
            return schema;
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar o schema JSON '" + caminho + "'", e);
        }
    }

    private static String normalizar(String caminho) {
        return caminho.contains(":") ? caminho : "classpath:" + caminho;
    }
}
