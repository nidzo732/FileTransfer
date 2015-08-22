package com.nidzo.filetransfer.cryptography;

public class Base64 {
    public static String Encode(byte[] realData) {
        return android.util.Base64.encodeToString(realData, android.util.Base64.NO_WRAP);
    }

    public static byte[] Decode(String data) {
        return android.util.Base64.decode(data, android.util.Base64.NO_WRAP);
    }
}