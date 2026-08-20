package io.payloadconverter.engine;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.mapping.model.MappingConfig;
import io.payloadconverter.schema.SchemaValidator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Decora o {@link MappingEngine} com validacao de JSON Schema (quando configurada no
 * fluxo): valida o payload de origem antes de converter e o payload de destino depois,
 * conforme {@link MappingConfig#schemaOrigem()}/{@link MappingConfig#schemaDestino()}.
 * <p>
 * E o {@link PayloadConverter} exposto para o resto da aplicacao (ex: a API REST) -
 * marcado {@code @Primary} para ser injetado no lugar do {@link MappingEngine} "puro",
 * que continua existindo como bean isolado e testavel sem depender de schemas.
 */
@Component
@Primary
public class SchemaValidatingConverter implements PayloadConverter {

    private final MappingEngine engine;
    private final SchemaValidator validador;

    public SchemaValidatingConverter(MappingEngine engine, SchemaValidator validador) {
        this.engine = engine;
        this.validador = validador;
    }

    @Override
    public JsonNode convert(JsonNode payloadOrigem, MappingConfig config) {
        if (config.schemaOrigem() != null && !config.schemaOrigem().isBlank()) {
            validador.validar(payloadOrigem, config.schemaOrigem(), config.id(), "origem");
        }

        JsonNode destino = engine.convert(payloadOrigem, config);

        if (config.schemaDestino() != null && !config.schemaDestino().isBlank()) {
            validador.validar(destino, config.schemaDestino(), config.id(), "destino");
        }

        return destino;
    }
}
