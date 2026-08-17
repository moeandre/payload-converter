package io.payloadconverter.component;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de todos os {@link MarketComponent} ("componentes de mercado") disponiveis no
 * classpath, descobertos via injecao de dependencia do Spring.
 */
@Component
public class ComponentRegistry {

    private final Map<String, MarketComponent> componentes;

    public ComponentRegistry(List<MarketComponent> componentesRegistrados) {
        Map<String, MarketComponent> mapa = new HashMap<>();
        for (MarketComponent componente : componentesRegistrados) {
            MarketComponent anterior = mapa.put(componente.nome(), componente);
            if (anterior != null) {
                throw new IllegalStateException(
                        "Componente de mercado duplicado: '" + componente.nome() + "' registrado por "
                                + anterior.getClass().getName() + " e " + componente.getClass().getName());
            }
        }
        this.componentes = Map.copyOf(mapa);
    }

    public MarketComponent obter(String nome) {
        MarketComponent componente = componentes.get(nome);
        if (componente == null) {
            throw new IllegalArgumentException(
                    "Componente de mercado desconhecido: '" + nome + "'. Disponiveis: " + componentes.keySet());
        }
        return componente;
    }
}
