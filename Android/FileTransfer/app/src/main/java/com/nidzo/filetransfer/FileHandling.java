package com.nidzo.filetransfer;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileHandling {
    public static final int FILE_SELECT_REQUEST_CODE = 1;
    private static final int MAX_FILE_LENGTH = 100 * 1024 * 1024;
    private static final int CHUNK_READ_LENGTH = 1024;

    private MainActivity owner;

    public FileHandling(MainActivity owner) {
        this.owner = owner;
    }

    private static String getFileName(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private static long getFileSize(Uri uri, Context context) {
        long result = -1;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return result;
    }

    private static File getAvailableFileInDownloadsWithName(String fileName) throws IOException {
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment
                .DIRECTORY_DOWNLOADS);
        if (!downloadsFolder.exists()) {
            if (!downloadsFolder.mkdir()) {
                throw new IOException();
            }
        }
        boolean hasExtension = fileName.contains(".");
        if (hasExtension) {
            String name = fileName.substring(0, fileName.lastIndexOf("."));
            String extension = fileName.substring(fileName.lastIndexOf("."));

            File checkerFile = new File(Environment.getExternalStoragePublicDirectory(Environment
                    .DIRECTORY_DOWNLOADS), fileName);
            int index = 0;
            while (checkerFile.exists()) {
                index++;
                fileName = name + "(" + Integer.toString(index) + ")" + extension;
                checkerFile = new File(Environment.getExternalStoragePublicDirectory(Environment
                        .DIRECTORY_DOWNLOADS), fileName);
            }
            return checkerFile;
        } else {
            File checkerFile = new File(Environment.getExternalStoragePublicDirectory(Environment
                    .DIRECTORY_DOWNLOADS), fileName);
            int index = 0;
            while (checkerFile.exists()) {
                index++;
                fileName = fileName + "(" + Integer.toString(index) + ")";
                checkerFile = new File(Environment.getExternalStoragePublicDirectory(Environment
                        .DIRECTORY_DOWNLOADS), fileName);
            }
            return checkerFile;
        }
    }

    private static boolean checkExternalStorage() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private static boolean checkAvailableMemoryInDownloads(long amount) {
        return Environment.getExternalStorageDirectory().getFreeSpace() > amount;
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public void selectFileToSend() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            owner.startActivityForResult(Intent.createChooser(intent, "Open file to send"), FILE_SELECT_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            DialogBoxes.showMessageBox("Error", "No file manager", owner);
        }
    }

    public String getFileName(Intent result) {
        Uri fileUri;
        if (result.getData() != null) fileUri = result.getData();
        else fileUri = result.getParcelableExtra(Intent.EXTRA_STREAM);
        return getFileName(fileUri, owner);
    }

    public byte[] getFileContents(Intent result) throws FileTransferException {
        Uri fileUri;
        if (result.getData() != null) fileUri = result.getData();
        else fileUri = result.getParcelableExtra(Intent.EXTRA_STREAM);

        try {
            InputStream fileStream = owner.getContentResolver().openInputStream(fileUri);

            byte[] buffer = new byte[(int) MAX_FILE_LENGTH + CHUNK_READ_LENGTH + 1];
            int fileLength = 0;
            int bytesAdded = 0;
            while (bytesAdded != -1) {
                fileLength += bytesAdded;
                if (fileLength > MAX_FILE_LENGTH) throw new IOException("File too big");
                bytesAdded = fileStream.read(buffer, fileLength, CHUNK_READ_LENGTH);
            }
            byte[] fileContents = new byte[fileLength];

            System.arraycopy(buffer, 0, fileContents, 0, fileLength);
            return fileContents;
        } catch (IOException error) {
            throw new FileTransferException("Error reading file " + error.toString() + "->" + error.getMessage());
        } catch (Exception error) {
            throw new FileTransferException("Unknown error reading file " + error.toString() + "->" + error.getMessage());
        }
    }


    public File saveFile(byte[] fileContents, String fileName) throws FileTransferException {
        if (!FileHandling.checkExternalStorage()) {
            throw new FileTransferException("Can't save file anywhere");
        }
        if (!FileHandling.checkAvailableMemoryInDownloads(fileContents.length)) {
            throw new FileTransferException("Not enough memory to save file");
        }
        File writtenFile;
        try {
            writtenFile = FileHandling.getAvailableFileInDownloadsWithName(fileName);
            FileOutputStream outStream = new FileOutputStream(writtenFile);
            outStream.write(fileContents);
            outStream.flush();
            outStream.close();
            return writtenFile;
        } catch (IOException error) {
            throw new FileTransferException("Error when saving file: " + error.toString() + "->" + error.getMessage());
        }
    }

    public void offerToOpenFile(File file) {
        Intent openFileIntent = new Intent();
        openFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openFileIntent.setAction(Intent.ACTION_VIEW);
        try

        {
            Uri fileUri = Uri.fromFile(file);
            String mimeType = FileHandling.getMimeType(fileUri.toString());
            if (mimeType == null) {
                throw new ActivityNotFoundException();
            }
            openFileIntent.setDataAndType(fileUri, mimeType);
            owner.startActivity(openFileIntent);
        } catch (ActivityNotFoundException error) {
            DialogBoxes.showMessageBox("Error", "No application available to open file", owner);
        }
    }
}