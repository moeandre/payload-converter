package io.payloadconverter.component.exemplo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import io.payloadconverter.component.MarketComponent;
import io.payloadconverter.engine.ConversionContext;
import io.payloadconverter.util.JsonValues;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Exemplo de "componente de mercado": calculo simplificado de premio de um seguro-auto,
 * combinando o valor do veiculo (lido do payload de origem) com uma aliquota e um
 * desconto de bonus vindos dos argumentos do YAML.
 * <p>
 * Serve apenas de referencia de como estruturar uma regra de negocio real - substitua/
 * adicione componentes proprios no mesmo estilo, implementando {@link MarketComponent}.
 * <pre>
 * - target: premio.valorFinal
 *   component:
 *     nome: seguroAuto.calculoPremio
 *     args:
 *       aliquotaBase: 0.045
 *       descontoBonusPorClasse: 0.05   # aplicado por classe de bonus (0-9)
 * </pre>
 * Espera, no payload de origem, {@code veiculo.valorFipe} (numero) e
 * {@code condutor.classeBonus} (numero, 0-9).
 */
@Component
public class CalculoPremioSeguroAutoComponent implements MarketComponent {

    @Override
    public String nome() {
        return "seguroAuto.calculoPremio";
    }

    @Override
    public JsonNode aplicar(Map<String, Object> args, ConversionContext ctx) {
        Double valorFipe = comoDouble(ctx.resolver("veiculo.valorFipe"));
        if (valorFipe == null) {
            return null;
        }
        double aliquotaBase = ((Number) args.getOrDefault("aliquotaBase", 0.05)).doubleValue();
        double descontoPorClasse = ((Number) args.getOrDefault("descontoBonusPorClasse", 0.0)).doubleValue();

        int classeBonus = (int) Math.max(0, Math.min(9, comoDoubleOuZero(ctx.resolver("condutor.classeBonus"))));
        double desconto = Math.min(1.0, descontoPorClasse * classeBonus);

        double premio = valorFipe * aliquotaBase * (1 - desconto);
        double premioArredondado = Math.round(premio * 100.0) / 100.0;
        return new DoubleNode(premioArredondado);
    }

    private static Double comoDouble(JsonNode node) {
        Object valor = JsonValues.paraValorJava(node);
        return valor instanceof Number n ? n.doubleValue() : null;
    }

    private static double comoDoubleOuZero(JsonNode node) {
        Double valor = comoDouble(node);
        return valor == null ? 0.0 : valor;
    }
}
