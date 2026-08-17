package io.payloadconverter.expression;

import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.util.JsonValues;

/** Avalia uma {@link Expr} (arvore ja parseada de um {@code when}) contra um {@link ConversionContext}. */
public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    public static boolean avaliar(Expr expr, ConversionContext ctx) {
        return switch (expr) {
            case Expr.And and -> avaliar(and.esquerda(), ctx) && avaliar(and.direita(), ctx);
            case Expr.Or or -> avaliar(or.esquerda(), ctx) || avaliar(or.direita(), ctx);
            case Expr.Not not -> !avaliar(not.expr(), ctx);
            case Expr.ExistsCheck existe -> ctx.resolver(existe.caminho()) != null;
            case Expr.Comparison cmp -> avaliarComparacao(cmp, ctx);
            case Expr.InList in -> avaliarIn(in, ctx);
            case Expr.PathRef path -> JsonValues.ehVerdadeiro(JsonValues.paraValorJava(ctx.resolver(path.caminho())));
            case Expr.Literal lit -> JsonValues.ehVerdadeiro(lit.valor());
        };
    }

    private static Object valorDe(Expr expr, ConversionContext ctx) {
        return switch (expr) {
            case Expr.Literal lit -> lit.valor();
            case Expr.PathRef path -> JsonValues.paraValorJava(ctx.resolver(path.caminho()));
            default -> avaliar(expr, ctx);
        };
    }

    private static boolean avaliarComparacao(Expr.Comparison cmp, ConversionContext ctx) {
        Object esquerda = valorDe(cmp.esquerda(), ctx);
        Object direita = valorDe(cmp.direita(), ctx);

        if (cmp.operador() == Expr.Operador.EQ || cmp.operador() == Expr.Operador.NEQ) {
            boolean iguais = saoIguais(esquerda, direita);
            return cmp.operador() == Expr.Operador.EQ ? iguais : !iguais;
        }

        Double e = paraNumero(esquerda);
        Double d = paraNumero(direita);
        if (e == null || d == null) {
            throw new ExpressionSyntaxException(
                    "Operador relacional (%s) exige valores numericos, recebeu '%s' e '%s'"
                            .formatted(cmp.operador(), esquerda, direita));
        }
        int c = Double.compare(e, d);
        return switch (cmp.operador()) {
            case GT -> c > 0;
            case GTE -> c >= 0;
            case LT -> c < 0;
            case LTE -> c <= 0;
            default -> throw new IllegalStateException("Operador inesperado: " + cmp.operador());
        };
    }

    private static boolean avaliarIn(Expr.InList in, ConversionContext ctx) {
        Object valor = valorDe(in.valor(), ctx);
        for (Expr candidato : in.candidatos()) {
            if (saoIguais(valor, valorDe(candidato, ctx))) {
                return true;
            }
        }
        return false;
    }

    private static boolean saoIguais(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        Double numA = paraNumero(a);
        Double numB = paraNumero(b);
        if (numA != null && numB != null) {
            return numA.doubleValue() == numB.doubleValue();
        }
        return a.toString().equals(b.toString());
    }

    private static Double paraNumero(Object valor) {
        if (valor instanceof Number n) {
            return n.doubleValue();
        }
        if (valor instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignorado) {
                return null;
            }
        }
        return null;
    }
}
