package com.umg.sigelab.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash SHA-256 en hexadecimal minusculas. Es el mismo formato que usa el script SQL
 * para los usuarios de prueba (HASHBYTES('SHA2_256', ...)).
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String textoPlano) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(textoPlano.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM.", e);
        }
    }

    public static boolean coincide(String textoPlano, String hashAlmacenado) {
        if (textoPlano == null || hashAlmacenado == null) {
            return false;
        }
        byte[] a = hash(textoPlano).getBytes(StandardCharsets.UTF_8);
        byte[] b = hashAlmacenado.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
