package com.nidzo.filetransfer.cryptography;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA512 {
    public static byte[] generateHash(byte[] data) throws CryptographyException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographyException("Crypto failure");
        }

    }
}