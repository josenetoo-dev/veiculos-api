package com.josenetoo_dev.veiculos_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.io.IOException;
import java.util.UUID;

@Component
public class ImageStorage {
    private static final Logger LOG = LoggerFactory.getLogger(ImageStorage.class);
    private final Path directory;
    private final String publicPrefix;
    public ImageStorage(@Value("${upload.dir}") String directory, @Value("${app.public-base-url}") String baseUrl) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.publicPrefix = baseUrl.replaceAll("/+$", "") + "/uploads/fotos/";
        if (publicPrefix.length() > 200) throw new IllegalStateException("URL pública de upload excede limite");
    }
    public String save(ImageValidator.ValidatedImage image) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("Upload exige transação");
        String name = UUID.randomUUID() + "." + image.extension();
        Path destination = directory.resolve(name);
        // Registrar antes da escrita cobre falha parcial e rollback após saveAll/commit do banco.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) delete(destination);
            }
        });
        try {
            Files.createDirectories(directory);
            Files.write(destination, image.bytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            delete(destination);
            throw new java.io.UncheckedIOException("Falha ao armazenar imagem", e);
        }
        return publicPrefix + name;
    }
    /**
     * Somente chamada após validar permissão e vínculo com AnuncioFoto no banco.
     * Nunca aceita caminhos arbitrários, inclusive links simbólicos.
     */
    public byte[] readAuthorized(String filename) {
        if (filename == null || !filename.matches(
                "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(jpg|png)")) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.FotoNaoEncontradaException("Foto não encontrada");
        }
        Path location = directory.resolve(filename).normalize();
        if (!location.startsWith(directory)) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.FotoNaoEncontradaException("Foto não encontrada");
        }
        try {
            if (!Files.isRegularFile(location, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(location) > ImageValidator.MAX_BYTES) {
                throw new com.josenetoo_dev.veiculos_api.exception.ex.FotoNaoEncontradaException("Foto não encontrada");
            }
            return Files.readAllBytes(location);
        } catch (IOException e) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.FotoNaoEncontradaException("Foto não encontrada");
        }
    }

    public void deleteAfterCommit(String url) {
        if (url == null || !url.startsWith(publicPrefix)) return;
        String name = url.substring(publicPrefix.length());
        if (!name.matches("[0-9a-fA-F-]{36}\\.(jpg|png)")) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { delete(directory.resolve(name)); }
        });
    }
    private void delete(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { LOG.error("Falha ao limpar arquivo de imagem; reconciliação de storage necessária"); }
    }
}
