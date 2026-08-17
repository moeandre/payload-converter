package io.payloadconverter.expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.payloadconverter.engine.ConversionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ConversionContext contexto(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return ConversionContext.raiz(node, MAPPER, "teste");
    }

    @Test
    void deveAvaliarIgualdadeDeString() throws Exception {
        ConversionContext ctx = contexto("{\"documento\":{\"tipo\":\"CPF\"}}");
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("documento.tipo == 'CPF'"), ctx)).isTrue();
    }

    @Test
    void deveAvaliarDiferente() throws Exception {
        ConversionContext ctx = contexto("{\"documento\":{\"tipo\":\"CNPJ\"}}");
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("documento.tipo != 'CPF'"), ctx)).isTrue();
    }

    @Test
    void deveAvaliarOperadorIn() throws Exception {
        ConversionContext ctx = contexto("{\"documento\":{\"tipo\":\"CNPJ\"}}");
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("documento.tipo in ['CPF', 'CNPJ']"), ctx)).isTrue();
    }

    @Test
    void deveAvaliarComparacaoNumerica() throws Exception {
        ConversionContext ctx = contexto("{\"idade\": 20}");
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("idade >= 18"), ctx)).isTrue();
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("idade < 18"), ctx)).isFalse();
    }

    @Test
    void deveAvaliarELogicoOuLogicoENegacao() throws Exception {
        ConversionContext ctx = contexto("{\"a\": true, \"b\": false}");
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("a && !b"), ctx)).isTrue();
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("!a || b"), ctx)).isFalse();
    }

    @Test
    void deveAvaliarExists() throws Exception {
        ConversionContext ctx = contexto("{\"documento\":{\"numero\":\"123\"}}");
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("exists(documento.numero)"), ctx)).isTrue();
        assertThat(ExpressionEvaluator.avaliar(ExpressionParser.parse("exists(documento.dataObito)"), ctx)).isFalse();
    }

    @Test
    void devePriorizarParenteses() throws Exception {
        ConversionContext ctx = contexto("{\"tipo\":\"CNPJ\",\"ativo\":true}");
        Expr expr = ExpressionParser.parse("(tipo == 'CPF' || tipo == 'CNPJ') && ativo");
        assertThat(ExpressionEvaluator.avaliar(expr, ctx)).isTrue();
    }

    @Test
    void deveLancarErroDeSintaxeParaExpressaoInvalida() {
        assertThrows(ExpressionSyntaxException.class, () -> ExpressionParser.parse("tipo == "));
    }
}
