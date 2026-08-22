package io.payloadconverter.encaminhamento;

/** O sistema de destino configurado para o fluxo nao respondeu (timeout, conexao recusada, DNS, etc). */
public class DestinoIndisponivelException extends RuntimeException {
    public DestinoIndisponivelException(String destino, Throwable causa) {
        super("Falha ao encaminhar requisicao para '" + destino + "': " + causa.getMessage(), causa);
    }
}
