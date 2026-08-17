package io.payloadconverter.api.dto;

/** Resumo de um fluxo de conversao disponivel, retornado por {@code GET /convert}. */
public record FluxoResumo(String id, String descricao, String mercado, int totalRegras) {
}
