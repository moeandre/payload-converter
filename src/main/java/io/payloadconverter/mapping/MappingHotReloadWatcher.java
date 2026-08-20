package io.payloadconverter.mapping;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * Observa, em background, o diretorio de onde os YAML de mapeamento sao lidos e chama
 * {@link MappingConfigRegistry#recarregar()} automaticamente quando algum {@code .yml}/
 * {@code .yaml} e criado, alterado ou removido - sem reiniciar a aplicacao.
 * <p>
 * So funciona quando a localizacao configurada resolve para um diretorio real em disco
 * (rodando via {@code mvn}/IDE a partir de {@code target/classes}, ou apontando para um
 * diretorio externo com {@code file:...}). Dentro de um JAR empacotado nao ha diretorio
 * real para observar - nesse caso o watcher fica inativo e a recarga so acontece via
 * {@code POST /admin/mappings/reload}. Desativavel via
 * {@code payload-converter.hot-reload.enabled=false}.
 */
@Component
public class MappingHotReloadWatcher {

    private static final Logger log = LoggerFactory.getLogger(MappingHotReloadWatcher.class);

    private final MappingConfigRegistry registry;
    private final boolean habilitado;
    private final long debounceMs;

    private Thread threadDeObservacao;
    private volatile boolean rodando = false;

    public MappingHotReloadWatcher(
            MappingConfigRegistry registry,
            @Value("${payload-converter.hot-reload.enabled:true}") boolean habilitado,
            @Value("${payload-converter.hot-reload.debounce-ms:300}") long debounceMs) {
        this.registry = registry;
        this.habilitado = habilitado;
        this.debounceMs = debounceMs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        if (!habilitado) {
            log.info("Hot-reload de configuracoes de mapeamento desativado (payload-converter.hot-reload.enabled=false)");
            return;
        }
        Path diretorio = resolverDiretorioObservavel();
        if (diretorio == null) {
            log.info("Hot-reload automatico inativo: '{}' nao resolve para um diretorio real em disco "
                            + "(app empacotada em jar?). Use POST /admin/mappings/reload para recarregar manualmente.",
                    registry.localizacao());
            return;
        }

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            diretorio.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            rodando = true;
            threadDeObservacao = new Thread(() -> observar(watchService), "mapping-hot-reload-watcher");
            threadDeObservacao.setDaemon(true);
            threadDeObservacao.start();
            log.info("Hot-reload automatico ativo, observando '{}'", diretorio);
        } catch (IOException e) {
            log.warn("Nao foi possivel iniciar o watcher de hot-reload em '{}': {}", diretorio, e.getMessage());
        }
    }

    private void observar(WatchService watchService) {
        try {
            while (rodando) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                boolean relevante = key.pollEvents().stream()
                        .map(evento -> String.valueOf(evento.context()))
                        .anyMatch(MappingHotReloadWatcher::ehArquivoDeMapeamento);
                key.reset();

                if (!relevante) {
                    continue;
                }

                // debounce: agrupa rajadas de eventos (ex: editor salvando varios arquivos) em uma unica recarga
                sleepSilencioso(debounceMs);
                drenarEventosPendentes(watchService);

                try {
                    registry.recarregar();
                } catch (Exception e) {
                    log.error("Hot-reload: falha ao recarregar configuracoes de mapeamento - mantendo a versao anterior. {}",
                            e.getMessage());
                }
            }
        } finally {
            fecharSilenciosamente(watchService);
        }
    }

    private static void drenarEventosPendentes(WatchService watchService) {
        WatchKey pendente;
        while ((pendente = watchService.poll()) != null) {
            pendente.pollEvents();
            pendente.reset();
        }
    }

    private static boolean ehArquivoDeMapeamento(String nomeArquivo) {
        String minusculo = nomeArquivo.toLowerCase(java.util.Locale.ROOT);
        return minusculo.endsWith(".yml") || minusculo.endsWith(".yaml");
    }

    /** Deriva o diretorio a observar a partir do padrao configurado (ex: "classpath:mappings/*.yml" -> "mappings/"). */
    private Path resolverDiretorioObservavel() {
        String localizacao = registry.localizacao();
        int ultimaBarra = localizacao.lastIndexOf('/');
        if (ultimaBarra < 0) {
            return null;
        }
        String localizacaoDoDiretorio = localizacao.substring(0, ultimaBarra + 1);
        try {
            Resource recurso = registry.resolver().getResource(localizacaoDoDiretorio);
            if (!recurso.exists()) {
                return null;
            }
            Path caminho = recurso.getFile().toPath();
            return java.nio.file.Files.isDirectory(caminho) ? caminho : null;
        } catch (IOException e) {
            return null; // tipicamente: recurso dentro de um jar, sem File real correspondente
        }
    }

    private static void sleepSilencioso(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void fecharSilenciosamente(WatchService watchService) {
        try {
            watchService.close();
        } catch (IOException ignorado) {
            // encerrando mesmo assim
        }
    }

    @PreDestroy
    public void parar() {
        rodando = false;
        if (threadDeObservacao != null) {
            threadDeObservacao.interrupt();
        }
    }
}
