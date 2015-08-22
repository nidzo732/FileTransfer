package com.nidzo.filetransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

public class DialogBoxes {

    public static void showMessageBox(String title, String text, Context context) {
        showMessageBox(title, text, null, context);
    }

    public static void showMessageBox(
            String title, String text, DialogInterface.OnClickListener onOkClick, Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage(text);
        dialog.setTitle(title);
        dialog.setPositiveButton("Ok", onOkClick);
        dialog.setCancelable(true);
        dialog.create().show();
    }

    public static void showConfirmationBox(
            String title, String text, DialogInterface.OnClickListener onYesClick, final DialogInterface.OnClickListener onNoClick, Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage(text);
        dialog.setTitle(title);
        dialog.setPositiveButton("Yes", onYesClick);
        dialog.setNegativeButton("No", onNoClick);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onNoClick.onClick(dialog, 0);
            }
        });
        dialog.setCancelable(true);
        dialog.create().show();
    }

    public static void showInputBox(
            String title, String text, DialogInterface.OnClickListener onOkClick, final DialogInterface.OnClickListener onCancelClick, Context context, EditText input) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage(text);
        dialog.setTitle(title);
        dialog.setPositiveButton("Ok", onOkClick);
        dialog.setNegativeButton("Cancel", onCancelClick);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onCancelClick.onClick(dialog, 0);
            }
        });
        dialog.setView(input);
        dialog.setCancelable(true);
        dialog.create().show();
    }

    public static String showInputBox(final String title, final String text, final Activity owner) {
        final EditText inputField = new EditText(owner);
        ReturningRunnable<String> inputRunnable = new ReturningRunnable<String>() {
            @Override
            public void run() {
                showInputBox(title, text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setResult(inputField.getText().toString());
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setResult(null);
                            }
                        },
                        owner,
                        inputField);
            }
        };
        owner.runOnUiThread(inputRunnable);
        return inputRunnable.getResult();
    }

    public static boolean showConfirmationBox(final String title, final String text, final Activity owner) {
        ReturningRunnable<Boolean> confirmationRunnable = new ReturningRunnable<Boolean>() {
            @Override
            public void run() {
                showConfirmationBox(title, text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setResult(true);
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setResult(false);
                            }
                        },
                        owner);
            }
        };
        owner.runOnUiThread(confirmationRunnable);
        return confirmationRunnable.getResult();
    }

}