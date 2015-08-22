package com.nidzo.filetransfer.cryptography;

import java.math.BigInteger;

public class NumberConversion {
    private static final BigInteger byteValue = new BigInteger("256");
    public static BigInteger bytesToBigInteger(byte[] bytes)
    {
        String[] numberContents = new String[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] < 0) {
                numberContents[i] = Integer.valueOf(bytes[i] + 256).toString();
            }
            else {
                numberContents[i] = Integer.valueOf(bytes[i]).toString();
            }
        }
        BigInteger number = new BigInteger("0");
        for (int i = 0; i < bytes.length; i++) {
            number = number.multiply(byteValue);
            BigInteger addedByte = new BigInteger(numberContents[i]);
            number = number.add(addedByte);
        }
        return number;
    }
    public static BigInteger stringToBigInteger(String contents) {
        return bytesToBigInteger(Base64.Decode(contents));
    }

    public static String bigIntegerToString(BigInteger integer) {
        int[] numberContents = new int[4096];
        int lp = 4096;
        while (!integer.equals(BigInteger.ZERO)) {
            lp--;
            int currentByte = integer.mod(byteValue).intValue();
            integer = integer.divide(byteValue);
            numberContents[lp] = currentByte;
        }
        byte[] returnedContents = new byte[numberContents.length - lp];
        for (int i = 0; i < returnedContents.length; i++) {
            if (numberContents[lp + i] >= 128) {
                returnedContents[i] = (byte) (numberContents[lp + i] - 256);
            }
            else {
                returnedContents[i] = (byte) (numberContents[lp + i]);
            }
        }
        return Base64.Encode(returnedContents);
    }
}