package io.payloadconverter.function;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de todas as {@link TransformFunction} disponiveis no classpath (built-in e
 * customizadas), descobertas via injecao de dependencia do Spring.
 */
@Component
public class FunctionRegistry {

    private final Map<String, TransformFunction> funcoes;

    public FunctionRegistry(List<TransformFunction> funcoesRegistradas) {
        Map<String, TransformFunction> mapa = new HashMap<>();
        for (TransformFunction funcao : funcoesRegistradas) {
            TransformFunction anterior = mapa.put(funcao.nome(), funcao);
            if (anterior != null) {
                throw new IllegalStateException(
                        "Funcao de transformacao duplicada: '" + funcao.nome() + "' registrada por "
                                + anterior.getClass().getName() + " e " + funcao.getClass().getName());
            }
        }
        this.funcoes = Map.copyOf(mapa);
    }

    public TransformFunction obter(String nome) {
        TransformFunction funcao = funcoes.get(nome);
        if (funcao == null) {
            throw new IllegalArgumentException(
                    "Funcao de transformacao desconhecida: '" + nome + "'. Disponiveis: " + funcoes.keySet());
        }
        return funcao;
    }
}
