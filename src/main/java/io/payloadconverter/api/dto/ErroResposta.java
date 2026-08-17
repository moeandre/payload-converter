package io.payloadconverter.api.dto;

import java.time.Instant;

/** Corpo padrao de resposta de erro da API. */
public record ErroResposta(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String fluxo,
        String target
) {
    public static ErroResposta de(int status, String erro, String mensagem, String fluxo, String target) {
        return new ErroResposta(Instant.now(), status, erro, mensagem, fluxo, target);
    }
}
