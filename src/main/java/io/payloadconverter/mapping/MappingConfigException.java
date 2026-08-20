package io.payloadconverter.mapping;

/** Falha ao carregar/interpretar arquivo(s) de configuracao de mapeamento (YAML invalido, id duplicado, etc). */
public class MappingConfigException extends RuntimeException {
    public MappingConfigException(String message) {
        super(message);
    }

    public MappingConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
