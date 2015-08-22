package com.nidzo.filetransfer;

import android.content.Context;

import java.util.UUID;

public class Identification {
    private static String name, guid;

    public static String getName(Context context) {
        if (name != null) return name;
        name = DataStorage.getStoredItem("name", context);
        if (name == null) {
            return "Unnamed device";
        } else {
            name = DataStorage.getStoredItem("name", context);
            return name;
        }
    }

    public static void setName(String newName, Context context) {
        name = newName;
        DataStorage.storeItem("name", name, context);
    }

    public static String getGuid(Context context) {
        if (guid != null) return guid;
        guid = DataStorage.getStoredItem("guid", context);
        if (guid != null) {
            return guid;
        } else {
            guid = UUID.randomUUID().toString();
            DataStorage.storeItem("guid", guid, context);
            return guid;
        }
    }
}
