package io.payloadconverter.expression;

import java.util.List;

/** Arvore sintatica abstrata da DSL de expressoes condicionais ({@code when}). */
public sealed interface Expr {

    record Literal(Object valor) implements Expr {
    }

    record PathRef(String caminho) implements Expr {
    }

    record ExistsCheck(String caminho) implements Expr {
    }

    record Not(Expr expr) implements Expr {
    }

    record And(Expr esquerda, Expr direita) implements Expr {
    }

    record Or(Expr esquerda, Expr direita) implements Expr {
    }

    record Comparison(Expr esquerda, Operador operador, Expr direita) implements Expr {
    }

    record InList(Expr valor, List<Expr> candidatos) implements Expr {
    }

    enum Operador { EQ, NEQ, GT, GTE, LT, LTE }
}
