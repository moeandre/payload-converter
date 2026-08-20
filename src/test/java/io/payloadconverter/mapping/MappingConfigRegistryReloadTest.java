package io.payloadconverter.mapping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MappingConfigRegistryReloadTest {

    private Path diretorioTemp;

    @AfterEach
    void limpar() throws IOException {
        if (diretorioTemp != null && Files.exists(diretorioTemp)) {
            try (var stream = Files.walk(diretorioTemp)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static Path escrever(Path dir, String nomeArquivo, String conteudo) throws IOException {
        return Files.writeString(dir.resolve(nomeArquivo), conteudo);
    }

    private static String localizacaoPara(Path dir) {
        return "file:" + dir.toString().replace('\\', '/') + "/*.yml";
    }

    @Test
    void deveRecarregarQuandoArquivoMuda() throws Exception {
        diretorioTemp = Files.createTempDirectory("mappings-reload-test");
        escrever(diretorioTemp, "fluxo.yml", """
                id: fluxo-teste
                mappings:
                  - target: a
                    source: a
                """);

        MappingConfigRegistry registry = new MappingConfigRegistry(
                new PathMatchingResourcePatternResolver(), localizacaoPara(diretorioTemp));

        assertThat(registry.listarIds()).containsExactly("fluxo-teste");
        assertThat(registry.obter("fluxo-teste").mappings()).hasSize(1);

        escrever(diretorioTemp, "fluxo.yml", """
                id: fluxo-teste
                mappings:
                  - target: a
                    source: a
                  - target: b
                    source: b
                """);

        registry.recarregar();

        assertThat(registry.obter("fluxo-teste").mappings()).hasSize(2);
    }

    @Test
    void deveManterConfiguracaoAnteriorSeRecargaFalhar() throws Exception {
        diretorioTemp = Files.createTempDirectory("mappings-reload-fail-test");
        escrever(diretorioTemp, "fluxo.yml", """
                id: fluxo-teste
                mappings:
                  - target: a
                    source: a
                """);

        MappingConfigRegistry registry = new MappingConfigRegistry(
                new PathMatchingResourcePatternResolver(), localizacaoPara(diretorioTemp));

        escrever(diretorioTemp, "fluxo.yml", "mappings: [ { target: a, source: a } ]"); // sem 'id' -> invalido

        assertThrows(MappingConfigException.class, registry::recarregar);

        assertThat(registry.obter("fluxo-teste")).isNotNull();
        assertThat(registry.obter("fluxo-teste").mappings()).hasSize(1);
    }
}
