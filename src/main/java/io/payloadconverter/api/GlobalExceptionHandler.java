package io.payloadconverter.api;

import io.payloadconverter.api.dto.ErroResposta;
import io.payloadconverter.engine.ConversionException;
import io.payloadconverter.expression.ExpressionSyntaxException;
import io.payloadconverter.mapping.MappingConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/** Traduz excecoes do motor de conversao em respostas HTTP estruturadas e acionaveis. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Fluxo de conversao ({@code id}) inexistente. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErroResposta> fluxoNaoEncontrado(NoSuchElementException e) {
        return responder(HttpStatus.NOT_FOUND, "fluxo_nao_encontrado", e.getMessage(), null, null);
    }

    /** Payload nao converteu: campo obrigatorio ausente, caminho invalido, funcao/componente desconhecido, etc. */
    @ExceptionHandler(ConversionException.class)
    public ResponseEntity<ErroResposta> falhaDeConversao(ConversionException e) {
        log.warn("Falha ao converter payload: {}", e.getMessage());
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "falha_conversao", e.getMessage(), e.getFlowId(), e.getTarget());
    }

    /** Expressao 'when' com sintaxe invalida (erro de configuracao). */
    @ExceptionHandler(ExpressionSyntaxException.class)
    public ResponseEntity<ErroResposta> expressaoInvalida(ExpressionSyntaxException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "expressao_invalida", e.getMessage(), null, null);
    }

    /** Falha ao (re)carregar YAML de mapeamento - ex: via POST /admin/mappings/reload com um arquivo invalido. */
    @ExceptionHandler(MappingConfigException.class)
    public ResponseEntity<ErroResposta> configuracaoInvalida(MappingConfigException e) {
        log.warn("Falha ao carregar configuracao de mapeamento: {}", e.getMessage());
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "configuracao_invalida", e.getMessage(), null, null);
    }

    /** JSON de entrada malformado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> jsonInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "json_invalido", "Corpo da requisicao nao e um JSON valido", null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> erroInesperado(Exception e) {
        log.error("Erro inesperado ao processar requisicao", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "erro_interno", "Erro interno inesperado", null, null);
    }

    private static ResponseEntity<ErroResposta> responder(HttpStatus status, String erro, String mensagem, String fluxo, String target) {
        return ResponseEntity.status(status).body(ErroResposta.de(status.value(), erro, mensagem, fluxo, target));
    }
}
