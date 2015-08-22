package com.nidzo.filetransfer.cryptography;
import com.nidzo.filetransfer.FileTransferException;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    public static String encrypt(String key, byte[] plaintext) throws CryptographyException {
        try {
            byte[] keyBytes = Base64.Decode(key);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] cipherText = cipher.doFinal(plaintext);
            byte[] iv = cipher.getIV();
            byte[] hashedPart = new byte[cipherText.length + iv.length];
            System.arraycopy(iv, 0, hashedPart, 0, iv.length);
            System.arraycopy(cipherText, 0, hashedPart, iv.length, cipherText.length);
            byte[] mac = generateHMAC(hashedPart, keyBytes);
            byte[] returnValue = new byte[cipherText.length + iv.length + mac.length];
            System.arraycopy(hashedPart, 0, returnValue, 0, hashedPart.length);
            System.arraycopy(mac, 0, returnValue, hashedPart.length, mac.length);
            return Base64.Encode(returnValue);
        }
        catch (GeneralSecurityException error) {
            throw new CryptographyException("Crypto Failiure");
        }
    }

    public static byte[] decrypt(String key, String ciphertext) throws CryptographyException {
        try {
            byte[] keyBytes = Base64.Decode(key);
            byte[] ciphertextBytes = Base64.Decode(ciphertext);
            byte[] hashedPart = new byte[ciphertextBytes.length - 32];
            byte[] iv = new byte[16];
            byte[] mac = new byte[32];
            byte[] encryptedBytes = new byte[ciphertextBytes.length - 32 - 16];
            System.arraycopy(ciphertextBytes, 0, iv, 0, 16);
            System.arraycopy(ciphertextBytes, 0, hashedPart, 0, hashedPart.length);
            System.arraycopy(ciphertextBytes, hashedPart.length, mac, 0, 32);
            System.arraycopy(ciphertextBytes, 16, encryptedBytes, 0, encryptedBytes.length);
            if (!verifyHMAC(hashedPart, keyBytes, mac)) {
                throw new CryptographyException("Bad HMAC");
            }
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(encryptedBytes);
        }
        catch (GeneralSecurityException e) {
            throw new CryptographyException("Crypto Failiure");
        }
    }

    private static byte[] generateHMAC(byte[] data, byte[] key) throws NoSuchAlgorithmException,
            InvalidKeyException {
        Mac macGenerator = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");
        macGenerator.init(keySpec);
        return macGenerator.doFinal(data);
    }

    private static boolean verifyHMAC(
            byte[] data, byte[] key, byte[] mac) throws NoSuchAlgorithmException,
            InvalidKeyException {
        byte[] calculatedMac = generateHMAC(data, key);
        boolean macOk = true;
        for (int i = 0; i < mac.length; i++) {
            macOk = macOk && calculatedMac[i] == mac[i];
        }
        return macOk;
    }
}