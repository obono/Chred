/*
 * Copyright (C) 2013 OBN-soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.obnsoft.chred;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class Utils {

    private static final byte[] MD5HEADER =
            {'P', 'E', 'T', 'I', 'T', 'C', 'O', 'M'};

    public static byte[] getMD5(byte[] data) {
        byte[] md5 = null;
        byte[] bytes = new byte[MD5HEADER.length + data.length];
        System.arraycopy(MD5HEADER, 0, bytes, 0, MD5HEADER.length);
        System.arraycopy(data, 0, bytes, MD5HEADER.length, data.length);
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance("MD5");
            md5 = digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }

    public static boolean loadFromStreamCommon(InputStream in, byte[] header, byte[] data) {
        byte[] myHeader = new byte[header.length];
        byte[] name = new byte[8];
        byte[] md5 = new byte[16];
        try {
            in.read(myHeader);
            if (!Arrays.equals(myHeader, header)) return false;
            in.read(name);
            in.read(md5);
            in.read(data);
            if (!Arrays.equals(md5, Utils.getMD5(data))) return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean saveToStreamCommon(
            OutputStream out, String strName, byte[] header, byte[] data) {
        byte[] name = new byte[8];
        Arrays.fill(name, (byte) 0);
        System.arraycopy(strName.getBytes(), 0, name, 0,
                (strName.length() > 8) ? 8 : strName.length());
        byte[] md5 = Utils.getMD5(data);

        try {
            out.write(header);
            out.write(name);
            out.write(md5);
            out.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static int dp2px(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static final char IDEOGRAPHICS_SPACE = 0x3000;

    public static String trimUni(String s){
        int len = s.length();
        int st = 0;
        char[] val = s.toCharArray();

        while (st < len && (val[st] <= ' ' || val[st] == IDEOGRAPHICS_SPACE)) {
            st++;
        }
        while (st < len && (val[len - 1] <= ' ' || val[len - 1] == IDEOGRAPHICS_SPACE)) {
            len--;
        }
        return (st > 0 || len < s.length()) ? s.substring(st, len) : s;
    }

    /*-----------------------------------------------------------------------*/

    public static void showYesNoDialog(
            Context context, int iconId, int titleId, int msgId,
            android.content.DialogInterface.OnClickListener listener) {
        showYesNoDialog(context, iconId, context.getText(titleId), msgId, listener);
    }

    public static void showYesNoDialog(
            Context context, int iconId, CharSequence title, int msgId,
            android.content.DialogInterface.OnClickListener listener) {
        new android.app.AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(title)
                .setMessage(msgId)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public static void showCustomDialog(
            Context context, int iconId, int titleId, View view,
            DialogInterface.OnClickListener listener) {
        final AlertDialog dlg = new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
        if (listener != null) {
            dlg.setButton(AlertDialog.BUTTON_NEGATIVE, context.getText(android.R.string.cancel),
                    (DialogInterface.OnClickListener) null);
        }
        if (view instanceof EditText) {
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        dlg.getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });
        }
        dlg.show();
    }

    public static void showToast(Context context, int msgId) {
        Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show();
    }

}
