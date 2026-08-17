package io.payloadconverter.expression;

/** Tipos de token reconhecidos pela DSL de expressoes condicionais ({@code when}). */
enum TokenType {
    PATH, STRING, NUMBER, TRUE, FALSE, NULL,
    AND, OR, NOT, EQ, NEQ, GT, GTE, LT, LTE, IN, EXISTS,
    LPAREN, RPAREN, LBRACKET, RBRACKET, COMMA, EOF
}
