package com.example.sensorlab.uiutils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
/**
 * A class for dialog
 * Code Inspiration from Anders Lindström's repo:
 * https://gits-15.sys.kth.se/anderslm/Ble-Gatt-Movesense-2.0
 */
public class MsgUtils {

    // short message
    public static void showToast(String msg, Context context) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    // alert message
    public static Dialog createDialog(String title, String msg, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(" Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // do nothing, just close the alert
            }
        });
        return builder.create();
    }
}

