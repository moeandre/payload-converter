package io.payloadconverter.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.payloadconverter.component.ComponentRegistry;
import io.payloadconverter.component.MarketComponent;
import io.payloadconverter.expression.Expr;
import io.payloadconverter.expression.ExpressionEvaluator;
import io.payloadconverter.expression.ExpressionParser;
import io.payloadconverter.function.FunctionRegistry;
import io.payloadconverter.function.TransformFunction;
import io.payloadconverter.mapping.model.MappingConfig;
import io.payloadconverter.mapping.model.MappingRule;
import io.payloadconverter.util.JsonValues;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Motor de conversao: percorre as {@link MappingRule} de uma {@link MappingConfig} e monta
 * o payload de destino.
 * <p>
 * Prioridade de resolucao de valor de uma regra: {@code component} &gt; {@code transform}
 * &gt; {@code source} &gt; {@code forEach} (ver {@link MappingRule}).
 * <p>
 * Expressoes {@code when} sao parseadas uma unica vez e cacheadas (o texto da expressao e
 * puro/determinístico), para que o custo de parsing nao se repita a cada conversao.
 */
@Component
public class MappingEngine implements PayloadConverter {

    private final ObjectMapper mapper;
    private final FunctionRegistry funcoes;
    private final ComponentRegistry componentes;
    private final ConcurrentMap<String, Expr> cacheDeCondicoes = new ConcurrentHashMap<>();

    public MappingEngine(ObjectMapper mapper, FunctionRegistry funcoes, ComponentRegistry componentes) {
        this.mapper = mapper;
        this.funcoes = funcoes;
        this.componentes = componentes;
    }

    @Override
    public JsonNode convert(JsonNode payloadOrigem, MappingConfig config) {
        if (payloadOrigem == null || payloadOrigem.isNull() || payloadOrigem.isMissingNode()) {
            throw new ConversionException(config.id(), null, "payload de origem vazio ou nulo");
        }
        ConversionContext contextoRaiz = ConversionContext.raiz(payloadOrigem, mapper, config.id());
        ObjectNode destino = mapper.createObjectNode();
        aplicarRegras(config.mappings(), contextoRaiz, destino, config.id());
        return destino;
    }

    private void aplicarRegras(List<MappingRule> regras, ConversionContext ctx, ObjectNode destino, String flowId) {
        for (MappingRule regra : regras) {
            aplicarRegra(regra, ctx, destino, flowId);
        }
    }

    private void aplicarRegra(MappingRule regra, ConversionContext ctx, ObjectNode destino, String flowId) {
        try {
            if (regra.when() != null && !regra.when().isBlank() && !condicaoSatisfeita(regra, ctx)) {
                return;
            }

            if (!regra.mappings().isEmpty() || regra.forEach() != null) {
                aplicarForEach(regra, ctx, destino, flowId);
                return;
            }

            JsonNode valor = resolverValor(regra, ctx, flowId);

            if (ehAusente(valor) && regra.defaultValue() != null) {
                valor = JsonValues.paraJsonNode(regra.defaultValue(), mapper);
            }

            if (ehAusente(valor)) {
                if (regra.required()) {
                    throw new ConversionException(flowId, regra.target(), mensagemAusencia(regra));
                }
                return;
            }

            PathWriter.write(destino, regra.target(), valor, mapper);
        } catch (ConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new ConversionException(flowId, regra.target(), e.getMessage(), e);
        }
    }

    private boolean condicaoSatisfeita(MappingRule regra, ConversionContext ctx) {
        Expr condicao = cacheDeCondicoes.computeIfAbsent(regra.when(), ExpressionParser::parse);
        return ExpressionEvaluator.avaliar(condicao, ctx);
    }

    private JsonNode resolverValor(MappingRule regra, ConversionContext ctx, String flowId) {
        if (regra.component() != null) {
            MarketComponent componente = componentes.obter(regra.component().nome());
            return componente.aplicar(regra.component().args(), ctx);
        }
        if (regra.transform() != null) {
            JsonNode valorBase = regra.source() != null ? ctx.resolver(regra.source()) : null;
            TransformFunction funcao = funcoes.obter(regra.transform().function());
            return funcao.aplicar(valorBase, regra.transform().args(), ctx);
        }
        if (regra.source() != null) {
            return ctx.resolver(regra.source());
        }
        throw new ConversionException(flowId, regra.target(),
                "regra invalida: informe 'source', 'transform', 'component' ou ('forEach' + 'mappings')");
    }

    private void aplicarForEach(MappingRule regra, ConversionContext ctx, ObjectNode destinoPai, String flowId) {
        if (regra.forEach() == null || regra.forEach().isBlank()) {
            throw new ConversionException(flowId, regra.target(), "regra com 'mappings' aninhados precisa informar 'forEach'");
        }
        JsonNode arrayOrigem = ctx.resolver(regra.forEach());
        if (ehAusente(arrayOrigem)) {
            if (regra.required()) {
                throw new ConversionException(flowId, regra.target(), "array obrigatorio ausente em '" + regra.forEach() + "'");
            }
            return;
        }
        if (!arrayOrigem.isArray()) {
            throw new ConversionException(flowId, regra.target(),
                    "'" + regra.forEach() + "' deveria ser um array, encontrado " + arrayOrigem.getNodeType());
        }

        ArrayNode arrayDestino = mapper.createArrayNode();
        for (JsonNode elemento : arrayOrigem) {
            ConversionContext ctxItem = ctx.comEscopo(regra.as(), elemento);
            ObjectNode itemDestino = mapper.createObjectNode();
            aplicarRegras(regra.mappings(), ctxItem, itemDestino, flowId);
            arrayDestino.add(itemDestino);
        }
        PathWriter.write(destinoPai, regra.target(), arrayDestino, mapper);
    }

    private static String mensagemAusencia(MappingRule regra) {
        return "valor obrigatorio ausente" + (regra.source() != null ? " (source='" + regra.source() + "')" : "");
    }

    private static boolean ehAusente(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode();
    }
}
