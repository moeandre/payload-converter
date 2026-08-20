package io.payloadconverter.mapping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class MappingHotReloadWatcherTest {

    private Path diretorioTemp;
    private MappingHotReloadWatcher watcher;

    @AfterEach
    void limpar() throws IOException {
        if (watcher != null) {
            watcher.parar();
        }
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

    private static void aguardarAte(BooleanSupplier condicao, long timeoutMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < limite) {
            if (condicao.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
    }

    @Test
    void deveRecarregarAutomaticamenteQuandoArquivoMuda() throws Exception {
        diretorioTemp = Files.createTempDirectory("hot-reload-watcher-test");
        escrever(diretorioTemp, "fluxo.yml", """
                id: fluxo-teste
                mappings:
                  - target: a
                    source: a
                """);

        MappingConfigRegistry registry = new MappingConfigRegistry(
                new PathMatchingResourcePatternResolver(), localizacaoPara(diretorioTemp));
        watcher = new MappingHotReloadWatcher(registry, true, 100);
        watcher.iniciar();

        assertThat(registry.obter("fluxo-teste").mappings()).hasSize(1);

        escrever(diretorioTemp, "fluxo.yml", """
                id: fluxo-teste
                mappings:
                  - target: a
                    source: a
                  - target: b
                    source: b
                """);

        aguardarAte(() -> registry.obter("fluxo-teste").mappings().size() == 2, 8000);

        assertThat(registry.obter("fluxo-teste").mappings()).hasSize(2);
    }
}
