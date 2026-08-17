package io.payloadconverter.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser recursivo-descendente para a DSL de expressoes condicionais usada no campo
 * {@code when} das regras de mapeamento.
 * <p>
 * Precedencia (da menor para a maior): {@code ||}, {@code &&}, {@code !}, comparacao/{@code in}.
 * <p>
 * Exemplos suportados:
 * <pre>
 *   documento.tipo == 'CPF'
 *   documento.tipo in ['CPF', 'CNPJ']
 *   idade >= 18 &amp;&amp; !exists(documento.dataObito)
 *   (documento.tipo == 'CPF' || documento.tipo == 'CNPJ') &amp;&amp; documento.numero
 * </pre>
 */
public final class ExpressionParser {

    private final List<Token> tokens;
    private int pos = 0;

    private ExpressionParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static Expr parse(String texto) {
        ExpressionParser parser = new ExpressionParser(new Lexer(texto).tokenizar());
        Expr expr = parser.orExpr();
        parser.esperar(TokenType.EOF);
        return expr;
    }

    private Expr orExpr() {
        Expr esquerda = andExpr();
        while (verifica(TokenType.OR)) {
            avancar();
            esquerda = new Expr.Or(esquerda, andExpr());
        }
        return esquerda;
    }

    private Expr andExpr() {
        Expr esquerda = unary();
        while (verifica(TokenType.AND)) {
            avancar();
            esquerda = new Expr.And(esquerda, unary());
        }
        return esquerda;
    }

    private Expr unary() {
        if (verifica(TokenType.NOT)) {
            avancar();
            return new Expr.Not(unary());
        }
        return comparison();
    }

    private Expr comparison() {
        Expr esquerda = operando();

        Expr.Operador operador = switch (atual().tipo()) {
            case EQ -> Expr.Operador.EQ;
            case NEQ -> Expr.Operador.NEQ;
            case GT -> Expr.Operador.GT;
            case GTE -> Expr.Operador.GTE;
            case LT -> Expr.Operador.LT;
            case LTE -> Expr.Operador.LTE;
            default -> null;
        };
        if (operador != null) {
            avancar();
            return new Expr.Comparison(esquerda, operador, operando());
        }

        if (verifica(TokenType.IN)) {
            avancar();
            esperar(TokenType.LBRACKET);
            List<Expr> candidatos = new ArrayList<>();
            if (!verifica(TokenType.RBRACKET)) {
                candidatos.add(operando());
                while (verifica(TokenType.COMMA)) {
                    avancar();
                    candidatos.add(operando());
                }
            }
            esperar(TokenType.RBRACKET);
            return new Expr.InList(esquerda, candidatos);
        }

        return esquerda;
    }

    private Expr operando() {
        Token t = atual();
        return switch (t.tipo()) {
            case LPAREN -> {
                avancar();
                Expr interno = orExpr();
                esperar(TokenType.RPAREN);
                yield interno;
            }
            case EXISTS -> {
                avancar();
                esperar(TokenType.LPAREN);
                Token caminho = esperar(TokenType.PATH);
                esperar(TokenType.RPAREN);
                yield new Expr.ExistsCheck(caminho.texto());
            }
            case PATH -> { avancar(); yield new Expr.PathRef(t.texto()); }
            case STRING -> { avancar(); yield new Expr.Literal(t.texto()); }
            case NUMBER -> { avancar(); yield new Expr.Literal(Double.parseDouble(t.texto())); }
            case TRUE -> { avancar(); yield new Expr.Literal(Boolean.TRUE); }
            case FALSE -> { avancar(); yield new Expr.Literal(Boolean.FALSE); }
            case NULL -> { avancar(); yield new Expr.Literal(null); }
            default -> throw new ExpressionSyntaxException(
                    "Token inesperado '" + t.texto() + "' (" + t.tipo() + ") na expressao");
        };
    }

    private Token atual() {
        return tokens.get(pos);
    }

    private boolean verifica(TokenType tipo) {
        return atual().tipo() == tipo;
    }

    private void avancar() {
        pos++;
    }

    private Token esperar(TokenType tipo) {
        if (!verifica(tipo)) {
            throw new ExpressionSyntaxException(
                    "Esperado " + tipo + " mas encontrou " + atual().tipo() + " ('" + atual().texto() + "')");
        }
        Token t = atual();
        avancar();
        return t;
    }
}
