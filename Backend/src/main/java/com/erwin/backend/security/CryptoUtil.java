
package com.erwin.backend.security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CryptoUtil {

    private static String secretKey() {
        String key = System.getenv("AES_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "La variable de entorno AES_KEY no está configurada (ver .env.example). " +
                    "Se usa para cifrar/descifrar las credenciales de base de datos guardadas " +
                    "por usuario y no puede tener un valor por defecto embebido en el código."
            );
        }
        return key;
    }

    private static String initVector() {
        String iv = System.getenv("AES_IV");
        if (iv == null || iv.isBlank()) {
            throw new IllegalStateException(
                    "La variable de entorno AES_IV no está configurada (ver .env.example)."
            );
        }
        return iv;
    }

    public static String encrypt(String plainText) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector().getBytes(StandardCharsets.UTF_8));
            SecretKeySpec key = new SecretKeySpec(secretKey().getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new RuntimeException("Error encrypting DB password", ex);
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector().getBytes(StandardCharsets.UTF_8));
            SecretKeySpec key = new SecretKeySpec(secretKey().getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("Error decrypting DB password", ex);
        }
    }
}
