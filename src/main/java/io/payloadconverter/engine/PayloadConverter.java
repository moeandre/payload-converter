package io.payloadconverter.engine;

import com.fasterxml.jackson.databind.JsonNode;
import io.payloadconverter.mapping.model.MappingConfig;

/** Contrato do motor de conversao: aplica uma {@link MappingConfig} sobre um payload de origem. */
public interface PayloadConverter {

    JsonNode convert(JsonNode payloadOrigem, MappingConfig config);
}
