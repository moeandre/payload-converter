package io.payloadconverter.schema;

import io.payloadconverter.engine.ConversionException;

/**
 * Payload (de origem ou de destino) nao atende ao JSON Schema configurado para o fluxo.
 * Subclasse de {@link ConversionException} para reaproveitar o mesmo tratamento HTTP (422).
 */
public class SchemaValidationException extends ConversionException {
    public SchemaValidationException(String flowId, String rotulo, String message) {
        super(flowId, rotulo, message);
    }
}
