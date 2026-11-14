package com.josenetoo_dev.veiculos_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class CpfProtectionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NONCE_BYTES = 12;
    private final byte[] key;

    public CpfProtectionService(@Value("${seller.pii.encryption-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) key = null;
        else {
            byte[] decoded;
            try { decoded = Base64.getDecoder().decode(encodedKey); }
            catch (IllegalArgumentException ex) { throw new IllegalStateException("SELLER_PII_KEY inválida"); }
            if (decoded.length != 32) throw new IllegalStateException("SELLER_PII_KEY deve codificar 32 bytes");
            key = Arrays.copyOf(decoded, decoded.length);
            Arrays.fill(decoded, (byte) 0);
        }
    }

    public boolean available() { return key != null; }

    public ProtectedCpf protect(String value) {
        if (!available()) throw new IllegalStateException("Proteção de dados pessoais indisponível");
        String cpf = CpfValidator.normalizeAndValidate(value);
        try {
            String ciphertext = encrypt(cpf, "auto-minas:cpf:v1");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update("auto-minas:cpf-lookup:v1:".getBytes(StandardCharsets.US_ASCII));
            String lookupHash = HexFormat.of().formatHex(mac.doFinal(cpf.getBytes(StandardCharsets.US_ASCII)));
            return new ProtectedCpf(ciphertext, lookupHash, cpf.substring(9));
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível proteger o dado pessoal");
        }
    }

    public String protectSellerName(String value) {
        if (!available()) throw new IllegalStateException("Proteção de dados pessoais indisponível");
        try { return encrypt(value, "auto-minas:seller-name:v1"); }
        catch (Exception ex) { throw new IllegalStateException("Não foi possível proteger o dado pessoal"); }
    }

    public String revealSellerName(String ciphertext) {
        if (!available() || ciphertext == null || ciphertext.isBlank()) throw new IllegalStateException("Proteção de dados pessoais indisponível");
        try {
            byte[] envelope = Base64.getDecoder().decode(ciphertext);
            if (envelope.length <= NONCE_BYTES + 16) throw new IllegalArgumentException();
            byte[] nonce = Arrays.copyOfRange(envelope, 0, NONCE_BYTES);
            byte[] encrypted = Arrays.copyOfRange(envelope, NONCE_BYTES, envelope.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD("auto-minas:seller-name:v1".getBytes(StandardCharsets.US_ASCII));
            byte[] plain = cipher.doFinal(encrypted);
            try { return new String(plain, StandardCharsets.UTF_8); }
            finally { Arrays.fill(envelope,(byte)0);Arrays.fill(encrypted,(byte)0);Arrays.fill(plain,(byte)0); }
        } catch (Exception ex) { throw new IllegalStateException("Não foi possível consultar os dados pessoais protegidos"); }
    }

    private String encrypt(String value, String aad) throws Exception {
        byte[] nonce = new byte[NONCE_BYTES]; RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(aad.getBytes(StandardCharsets.US_ASCII));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] envelope = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, envelope, 0, nonce.length);
        System.arraycopy(encrypted, 0, envelope, nonce.length, encrypted.length);
        Arrays.fill(encrypted, (byte) 0);
        String ciphertext = Base64.getEncoder().encodeToString(envelope);
        Arrays.fill(envelope, (byte) 0);
        return ciphertext;
    }

    public record ProtectedCpf(String ciphertext, String lookupHash, String lastDigits) {}
}
