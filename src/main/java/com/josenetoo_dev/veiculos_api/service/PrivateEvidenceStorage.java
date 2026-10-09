package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.ArmazenamentoPrivadoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * Evidências confidenciais com AES-256-GCM em diretório não público.
 * A chave deve ser fornecida pelo secret manager: VERIFICATION_STORAGE_KEY.
 * Sem chave, upload/download devolvem 503 (fail closed).
 *
 * NÃO usar upload.dir nem servir este diretório por WebMvcConfigurer.
 */
@Component
public class PrivateEvidenceStorage {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NONCE_BYTES = 12;
    private final Path directory;
    private final SecretKeySpec encryptionKey;

    public PrivateEvidenceStorage(
            @Value("${verification.storage.dir:private-verification-documents}") String path,
            @Value("${verification.storage.key:}") String key,
            @Value("${upload.dir:uploads/fotos}") String publicUploadDirectory) {
        directory = Path.of(path).toAbsolutePath().normalize();
        Path publicDirectory = Path.of(publicUploadDirectory).toAbsolutePath().normalize();
        if (directory.startsWith(publicDirectory)) {
            throw new IllegalStateException("Documentos privados não podem ficar no diretório de fotos públicas");
        }
        if (key == null || key.isBlank()) {
            encryptionKey = null;
        } else {
            byte[] raw;
            try { raw = Base64.getDecoder().decode(key); }
            catch (IllegalArgumentException e) { throw new IllegalStateException("Chave privada inválida"); }
            if (raw.length != 32) throw new IllegalStateException("Chave privada AES deve ter 32 bytes");
            encryptionKey = new SecretKeySpec(raw, "AES");
            Arrays.fill(raw, (byte) 0);
        }
    }

    public boolean available() { return encryptionKey != null; }

    private void requireAvailable() {
        if (!available()) throw new ArmazenamentoPrivadoIndisponivelException();
    }

    private Path file(String id) {
        if (id == null || !id.matches("[a-f0-9-]{36}")) {
            throw new IllegalArgumentException("Identificador de arquivo inválido");
        }
        return directory.resolve(id + ".enc");
    }

    public String store(ImageValidator.ValidatedImage image) {
        requireAvailable();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Armazenamento privado exige transação");
        }
        String id = UUID.randomUUID().toString();
        Path path = file(id);
        byte[] iv = new byte[NONCE_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            cipher.updateAAD(id.getBytes(StandardCharsets.US_ASCII));
            byte[] ciphertext = cipher.doFinal(image.bytes());
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) deleteFile(path);
                }
            });
            Files.createDirectories(directory);
            Files.write(path, encrypted, StandardOpenOption.CREATE_NEW);
            Arrays.fill(encrypted, (byte) 0);
            return id;
        } catch (Exception e) {
            deleteFile(path);
            throw new IllegalStateException("Falha ao armazenar evidência criptografada", e);
        }
    }

    public byte[] read(String id) {
        requireAvailable();
        Path path = file(id);
        try {
            byte[] encrypted = Files.readAllBytes(path);
            if (encrypted.length <= NONCE_BYTES + 16 || encrypted.length > ImageValidator.MAX_BYTES * 2L) {
                throw new IllegalStateException("Arquivo de evidência inválido");
            }
            byte[] iv = Arrays.copyOfRange(encrypted, 0, NONCE_BYTES);
            byte[] payload = Arrays.copyOfRange(encrypted, NONCE_BYTES, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            cipher.updateAAD(id.getBytes(StandardCharsets.US_ASCII));
            return cipher.doFinal(payload);
        } catch (Exception e) {
            // Não vazar path/nomes de arquivos em logs ou HTTP.
            throw new IllegalStateException("Falha de leitura da evidência criptografada");
        }
    }

    public void removeAfterCommit(String id) {
        Path path = file(id);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Exclusão de evidência exige transação");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { deleteFile(path); }
        });
    }

    private static void deleteFile(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException ignored) { /* Analisar e conciliar arquivos órfãos operacionalmente. */ }
    }
}
