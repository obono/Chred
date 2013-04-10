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

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

public class Utils {

    public static final char IDEOGRAPHICS_SPACE = 0x3000;
    public static final String LF = "\r\n";

    public static String getVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            return "Version ".concat(packageInfo.versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int dp2px(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

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

    public static byte[] getMD5(byte[] data) {
        byte[] md5 = null;
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance("MD5");
            md5 = digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }

    public static String extractString(byte[] data, int start, int len) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < len; i++) {
            char c = (char) data[start + i];
            if (c >= 'a') c -= 0x20;
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c == '_') buf.append(c);
        }
        return buf.toString();
    }

    public static void embedString(byte[] data, int start, int len, String str) {
        Arrays.fill(data, start, start + len, (byte) 0);
        System.arraycopy(str.getBytes(), 0, data, start,
                (str.length() > len) ? len : str.length());
    }

    public static int extractValue(byte[] data, int start, int len) {
        int val = 0;
        for (int i = 0; i < len; i++) {
            val |= (data[start + i] & 0xFF) << i * 8;
        }
        return val;
    }

    public static void embedValue(byte[] ary, int start, int len, int val) {
        for (int i = 0; i < len; i++) {
            ary[start + i] = (byte) (val & 0xFF);
            val >>= 8;
        }
    }

    /*-----------------------------------------------------------------------*/

    public static void showYesNoDialog(
            Context context, int iconId, int titleId, String msg, OnClickListener listener) {
        show2ButtonsDialog(context, iconId, titleId, msg,
                android.R.string.no, android.R.string.yes, listener);
    }

    public static void showShareDialog(
            final Context context, int iconId, int titleId, String msg, final String path) {
        final String mimetype = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(path.substring(path.lastIndexOf('.') + 1));
        boolean showNeutral = (mimetype != null);
        OnClickListener l = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.fromFile(new File(path));
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType((mimetype == null) ? "application/octet-stream" : mimetype);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                } else {
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, mimetype);
                }
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    showToast(context, R.string.msg_error);
                    e.printStackTrace();
                }
            }
        };
        if (showNeutral) {
            show3ButtonsDialog(context, iconId, titleId, msg,
                    android.R.string.ok, R.string.view, R.string.share, l, l);
        } else {
            show2ButtonsDialog(context, iconId, titleId, msg,
                    android.R.string.ok, R.string.share, l);
        }
    }

    public static void show2ButtonsDialog(
            Context context, int iconId, int titleId, String msg,
            int ngBtnId, int psBtnId, OnClickListener psLsn) {
        new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setMessage(msg)
                .setNegativeButton(ngBtnId, null)
                .setPositiveButton(psBtnId, psLsn)
                .show();
    }

    public static void show3ButtonsDialog(
            Context context, int iconId, int titleId, String msg,
            int ngBtnId, int mdBtnId, int psBtnId, OnClickListener mdLsn, OnClickListener psLsn) {
        new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setMessage(msg)
                .setNegativeButton(ngBtnId, null)
                .setNeutralButton(mdBtnId, mdLsn)
                .setPositiveButton(psBtnId, psLsn)
                .show();
    }

    public static void showCustomDialog(
            Context context, int iconId, int titleId, View view, OnClickListener listener) {
        final AlertDialog dlg = new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
        if (listener != null) {
            dlg.setButton(AlertDialog.BUTTON_NEGATIVE,
                    context.getText(android.R.string.cancel), (OnClickListener) null);
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

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

}
