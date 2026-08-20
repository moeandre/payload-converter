package io.payloadconverter.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SchemaValidator validador = new SchemaValidator(new DefaultResourceLoader());

    @Test
    void deveAceitarPayloadValido() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "documento": { "numero": "123", "tipo": "CPF" } }
                """);
        assertDoesNotThrow(() -> validador.validar(payload, "schemas/sistemaA.schema.json", "teste", "origem"));
    }

    @Test
    void deveRejeitarPayloadComEnumInvalido() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "documento": { "numero": "123", "tipo": "PASSAPORTE" } }
                """);
        assertThrows(SchemaValidationException.class,
                () -> validador.validar(payload, "schemas/sistemaA.schema.json", "teste", "origem"));
    }

    @Test
    void deveRejeitarClasseBonusForaDoIntervalo() throws Exception {
        JsonNode payload = mapper.readTree("""
                { "condutor": { "classeBonus": 99 } }
                """);
        assertThrows(SchemaValidationException.class,
                () -> validador.validar(payload, "schemas/sistemaA.schema.json", "teste", "origem"));
    }
}
