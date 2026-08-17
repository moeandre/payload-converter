package io.payloadconverter.expression;

/** Um token lexico com seu tipo e o texto original (sem aspas, no caso de STRING). */
record Token(TokenType tipo, String texto) {
}
