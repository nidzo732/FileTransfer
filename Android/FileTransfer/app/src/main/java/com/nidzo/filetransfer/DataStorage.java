package com.nidzo.filetransfer;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class DataStorage {

    private static final String prefsName = "FileTransferPreferences";

    public static void storeItem(String name, String value, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(name, value);
        prefsEditor.apply();
    }

    public static String getStoredItem(String name, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        return prefs.getString(name, null);
    }

    public static void storeDataToFile(String name, String data, Context context) {
        try {
            FileOutputStream outputStream = context.openFileOutput(name, Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (IOException ignored) {

        }
    }

    public static String readDataFromFile(String name, Context context) {
        try {
            FileInputStream inputStream = context.openFileInput(name);
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream));
            String line, contents = "";
            while ((line = fileReader.readLine()) != null) {
                contents += line;
            }
            return contents;
        } catch (IOException e) {
            return null;
        }
    }
}