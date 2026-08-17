package io.payloadconverter.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizador da DSL de expressoes condicionais. Suporta:
 * <ul>
 *     <li>caminhos: {@code documento.tipo}, {@code enderecos[0].cidade}</li>
 *     <li>literais: strings {@code 'x'}/{@code "x"}, numeros, {@code true}/{@code false}/{@code null}</li>
 *     <li>operadores: {@code == != > >= < <= && || ! in exists(...)}</li>
 *     <li>agrupamento: {@code ( ) [ ] ,}</li>
 * </ul>
 */
final class Lexer {

    private static final Pattern CAMINHO =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*|\\[\\d+])*");
    private static final Pattern NUMERO = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Map<String, TokenType> PALAVRAS_CHAVE = Map.of(
            "true", TokenType.TRUE,
            "false", TokenType.FALSE,
            "null", TokenType.NULL,
            "in", TokenType.IN,
            "exists", TokenType.EXISTS
    );

    private final String origem;
    private int pos = 0;

    Lexer(String origem) {
        this.origem = origem == null ? "" : origem;
    }

    List<Token> tokenizar() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            pularEspacos();
            if (pos >= origem.length()) {
                tokens.add(new Token(TokenType.EOF, ""));
                return tokens;
            }
            char c = origem.charAt(pos);
            switch (c) {
                case '(' -> { tokens.add(new Token(TokenType.LPAREN, "(")); pos++; }
                case ')' -> { tokens.add(new Token(TokenType.RPAREN, ")")); pos++; }
                case '[' -> { tokens.add(new Token(TokenType.LBRACKET, "[")); pos++; }
                case ']' -> { tokens.add(new Token(TokenType.RBRACKET, "]")); pos++; }
                case ',' -> { tokens.add(new Token(TokenType.COMMA, ",")); pos++; }
                case '\'', '"' -> tokens.add(lerString(c));
                default -> tokens.add(lerOperadorOuLiteral());
            }
        }
    }

    private Token lerOperadorOuLiteral() {
        char c = origem.charAt(pos);
        if (c == '&' && peek(1) == '&') { pos += 2; return new Token(TokenType.AND, "&&"); }
        if (c == '|' && peek(1) == '|') { pos += 2; return new Token(TokenType.OR, "||"); }
        if (c == '!' && peek(1) == '=') { pos += 2; return new Token(TokenType.NEQ, "!="); }
        if (c == '!') { pos += 1; return new Token(TokenType.NOT, "!"); }
        if (c == '=' && peek(1) == '=') { pos += 2; return new Token(TokenType.EQ, "=="); }
        if (c == '>' && peek(1) == '=') { pos += 2; return new Token(TokenType.GTE, ">="); }
        if (c == '>') { pos += 1; return new Token(TokenType.GT, ">"); }
        if (c == '<' && peek(1) == '=') { pos += 2; return new Token(TokenType.LTE, "<="); }
        if (c == '<') { pos += 1; return new Token(TokenType.LT, "<"); }

        Matcher numero = NUMERO.matcher(origem).region(pos, origem.length());
        if (numero.lookingAt() && (Character.isDigit(c) || pareceNumeroNegativo())) {
            String texto = numero.group();
            pos += texto.length();
            return new Token(TokenType.NUMBER, texto);
        }

        Matcher caminho = CAMINHO.matcher(origem).region(pos, origem.length());
        if (caminho.lookingAt()) {
            String texto = caminho.group();
            pos += texto.length();
            TokenType palavraChave = PALAVRAS_CHAVE.get(texto.toLowerCase());
            return new Token(palavraChave != null ? palavraChave : TokenType.PATH, texto);
        }

        throw new ExpressionSyntaxException(
                "Caractere inesperado '" + c + "' na posicao " + pos + " da expressao: " + origem);
    }

    private boolean pareceNumeroNegativo() {
        return origem.charAt(pos) == '-' && pos + 1 < origem.length() && Character.isDigit(origem.charAt(pos + 1));
    }

    private Token lerString(char aspas) {
        int inicio = ++pos; // pula a aspa de abertura
        StringBuilder sb = new StringBuilder();
        while (pos < origem.length() && origem.charAt(pos) != aspas) {
            char c = origem.charAt(pos);
            if (c == '\\' && pos + 1 < origem.length()) {
                sb.append(origem.charAt(pos + 1));
                pos += 2;
            } else {
                sb.append(c);
                pos++;
            }
        }
        if (pos >= origem.length()) {
            throw new ExpressionSyntaxException("String nao terminada a partir da posicao " + inicio + " em: " + origem);
        }
        pos++; // pula a aspa de fechamento
        return new Token(TokenType.STRING, sb.toString());
    }

    private void pularEspacos() {
        while (pos < origem.length() && Character.isWhitespace(origem.charAt(pos))) {
            pos++;
        }
    }

    private char peek(int offset) {
        int i = pos + offset;
        return i < origem.length() ? origem.charAt(i) : '\0';
    }
}
