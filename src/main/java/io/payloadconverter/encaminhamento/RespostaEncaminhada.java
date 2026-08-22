package io.payloadconverter.encaminhamento;

import org.springframework.http.MediaType;

/**
 * Resposta bruta recebida do sistema de destino ao encaminhar um payload convertido -
 * capturada tal como veio (status, content-type e corpo em bytes), para ser devolvida de
 * forma transparente ao chamador original do orquestrador.
 */
public record RespostaEncaminhada(int status, MediaType contentType, byte[] corpo) {
}
