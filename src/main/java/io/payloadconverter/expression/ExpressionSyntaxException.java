package io.payloadconverter.expression;

/** Erro de sintaxe ao interpretar uma expressao condicional ({@code when}) do YAML de mapeamento. */
public class ExpressionSyntaxException extends RuntimeException {
    public ExpressionSyntaxException(String message) {
        super(message);
    }
}
