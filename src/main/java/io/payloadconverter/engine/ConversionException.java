package io.payloadconverter.engine;

/**
 * Erro de negocio ocorrido durante a conversao de um payload: caminho invalido,
 * campo obrigatorio ausente, funcao/componente desconhecido, etc.
 * <p>
 * Sempre carrega o {@code flowId} e, quando aplicavel, o {@code target} da regra que
 * falhou, para que a resposta HTTP e os logs sejam acionaveis.
 */
public class ConversionException extends RuntimeException {

    private final String flowId;
    private final String target;

    public ConversionException(String flowId, String target, String message) {
        super(mensagem(flowId, target, message));
        this.flowId = flowId;
        this.target = target;
    }

    public ConversionException(String flowId, String target, String message, Throwable cause) {
        super(mensagem(flowId, target, message), cause);
        this.flowId = flowId;
        this.target = target;
    }

    private static String mensagem(String flowId, String target, String message) {
        StringBuilder sb = new StringBuilder();
        if (flowId != null) {
            sb.append("[fluxo=").append(flowId).append("] ");
        }
        if (target != null) {
            sb.append("[target=").append(target).append("] ");
        }
        sb.append(message);
        return sb.toString();
    }

    public String getFlowId() {
        return flowId;
    }

    public String getTarget() {
        return target;
    }
}
