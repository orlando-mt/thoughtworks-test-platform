package com.thoughtworks.problem1application.infrastructure.github;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/** Lee llaves RSA en PEM, tanto PKCS#8 (BEGIN PRIVATE KEY) como PKCS#1 (BEGIN RSA PRIVATE KEY, el formato de GitHub). */
final class PemKeys {

    private static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";

    // AlgorithmIdentifier de rsaEncryption (OID 1.2.840.113549.1.1.1) con parámetros NULL
    private static final byte[] RSA_ALGORITHM_ID = { 0x30, 0x0D, 0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86,
            (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00 };

    private PemKeys() {
    }

    static PrivateKey readPrivateKey(String pem) {
        boolean pkcs1 = pem.contains(PKCS1_HEADER);
        String base64 = pem.replaceAll("-----(BEGIN|END) (RSA )?PRIVATE KEY-----", "").replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        if (pkcs1) {
            der = pkcs1ToPkcs8(der);
        }
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid GitHub App private key", e);
        }
    }

    /** PKCS#8 = SEQUENCE { INTEGER 0, AlgorithmIdentifier, OCTET STRING { llave PKCS#1 } } */
    private static byte[] pkcs1ToPkcs8(byte[] pkcs1) {
        byte[] version = { 0x02, 0x01, 0x00 };
        byte[] octetString = concat(new byte[] { 0x04 }, derLength(pkcs1.length), pkcs1);
        byte[] body = concat(version, RSA_ALGORITHM_ID, octetString);
        return concat(new byte[] { 0x30 }, derLength(body.length), body);
    }

    private static byte[] derLength(int length) {
        if (length < 0x80) {
            return new byte[] { (byte) length };
        }
        if (length <= 0xFF) {
            return new byte[] { (byte) 0x81, (byte) length };
        }
        if (length <= 0xFFFF) {
            return new byte[] { (byte) 0x82, (byte) (length >> 8), (byte) length };
        }
        return new byte[] { (byte) 0x83, (byte) (length >> 16), (byte) (length >> 8), (byte) length };
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}