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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

public class MainActivity extends TabActivity {

    private static final String TABTAG_TARGET = "target";
    private static final String TABTAG_EDIT = "edit";
    private static final String TABTAG_PALETTE = "palette";

    private static final String[] PRESET_FNAMES = {
        "Default palette", "SPU0", "SPU1", "SPU2", "SPU3", "BGF0",
    };

    private static final int REQUEST_ID_IMPORT_FILE = 1;
    private static final int REQUEST_ID_IMPORT_GALLERY = 2;
    //private static final int REQUEST_ID_IMPORT_CAMERA = 3;
    private static final int REQUEST_ID_EXPORT_CHR = 11;
    private static final int REQUEST_ID_EXPORT_COL = 12;

    private static final char FULLWIDTH_EXCLAMATION_MARK = 0xFF01;

    private MyApplication mApp;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mApp = (MyApplication) getApplication();
        myAddTab(TABTAG_TARGET, R.string.target, R.drawable.ic_tab_chr,    ChrsActivity.class);
        myAddTab(TABTAG_EDIT,   R.string.edit,   R.drawable.ic_tab_edit,   EditActivity.class);
        myAddTab(TABTAG_PALETTE,R.string.palette,R.drawable.ic_tab_palette,PaletteActivity.class);
        if (mApp.mCurTab != null) {
            getTabHost().setCurrentTabByTag(mApp.mCurTab);
        }
    }

    private void myAddTab(String tag, int mesId, int iconId, Class<?> cls) {
        TabHost tabHost = getTabHost();
        View view = View.inflate(this, R.layout.tab, null);
        Intent intent = new Intent().setClass(this, cls);;
        ((TextView) view.findViewById(R.id.tab_text)).setText(mesId);
        ((ImageView) view.findViewById(R.id.tab_icon)).setImageResource(iconId);
        tabHost.addTab(tabHost.newTabSpec(tag).setIndicator(view).setContent(intent));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApp.mCurTab = getTabHost().getCurrentTabTag();
        mApp.saveData();
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        switch (menuId) {
        case R.id.menu_import_file:
            requestFileToImport();
            return true;
        case R.id.menu_import_gallery:
            startActivityForResult(
                    new Intent(this, ScanQRActivity.class), REQUEST_ID_IMPORT_GALLERY);
            return true;
        case R.id.menu_import_preset:
            selectPresetToImport();
            return true;
        case R.id.menu_export_chr:
        case R.id.menu_export_col:
            requestFileToExport(menuId);
            return true;
        case R.id.menu_export_qr_chr:
        case R.id.menu_export_qr_col:
            confirmExportToQRCodes(menuId);
            return true;
        case R.id.menu_version:
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ID_IMPORT_FILE:
            if (resultCode == RESULT_OK) {
                executeImportFromFile(
                        data.getStringExtra(MyFilePickerActivity.INTENT_EXTRA_SELECTPATH));
            }
            break;
        case REQUEST_ID_IMPORT_GALLERY:
            if (resultCode == RESULT_OK) {
                confirmImportFromStream(new ByteArrayInputStream(
                        data.getByteArrayExtra(ScanQRActivity.INTENT_EXTRA_DATA)));
            }
            break;
        case REQUEST_ID_EXPORT_CHR:
        case REQUEST_ID_EXPORT_COL:
            if (resultCode == RESULT_OK) {
                confirmExportToFile(requestCode,
                        data.getStringExtra(MyFilePickerActivity.INTENT_EXTRA_SELECTPATH));
            }
            break;
        }
    }

    /*-----------------------------------------------------------------------*/

    private void requestFileToImport() {
        final Intent intent = new Intent(this, MyFilePickerActivity.class);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_TITLEID, R.string.title_import);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_DIRECTORY,
                MyFilePickerActivity.DEFAULT_DIR);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_EXTENSION, MyApplication.FNAMEEXT_PTC);
        startActivityForResult(intent, REQUEST_ID_IMPORT_FILE);
    }

    private void executeImportFromFile(String path) {
        try {
            confirmImportFromStream(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void selectPresetToImport() {
        OnClickListener cl = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String fname;
                if (which == 0) {
                    fname = "palette.ptc";
                } else {
                    fname = PRESET_FNAMES[which].toLowerCase(Locale.US)
                            .concat(MyApplication.FNAMEEXT_PTC);
                }
                try {
                    confirmImportFromStream(getResources().getAssets().open(fname));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_import_preset)
                .setItems(PRESET_FNAMES, cl)
                .show();
    }

    private void confirmImportFromStream(InputStream in) {
        boolean ret = false;
        try {
            PTCFile ptcfile = new PTCFile();
            ret = ptcfile.load(in);
            in.close();
            if (ret) {
                executeImportFromPTCFile(ptcfile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!ret) {
            Utils.showToast(this, R.string.msg_error);
        }
    }

    private void executeImportFromPTCFile(final PTCFile ptcfile) {
        final String pname = ptcfile.getNameWithType();
        OnClickListener cl = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (ptcfile.getType() == PTCFile.PTC_TYPE_CHR) {
                    mApp.mChrData.deserialize(ptcfile.getData());
                    refreshActivity();
                } else if (ptcfile.getType() == PTCFile.PTC_TYPE_COL) {
                    mApp.mColData.deserialize(ptcfile.getData());
                    refreshActivity();
                }
                String msg = String.format(getString(R.string.msg_loadptc), pname);
                Utils.showToast(MainActivity.this, msg);
            }
        };
        if (pname != null) {
            String msg = String.format(getString(R.string.msg_import), pname);
            Utils.showYesNoDialog(
                    this, R.drawable.ic_import, R.string.menu_import, msg, cl);
        } else {
            Utils.showToast(this, R.string.msg_notsupported);
        }
    }

    private void requestFileToExport(int menuId) {
        int titleId;
        String path;
        int requestCode;
        if (menuId == R.id.menu_export_col) {
            titleId = R.string.title_export_col;
            path = MyFilePickerActivity.DEFAULT_DIR_COL;
            requestCode = REQUEST_ID_EXPORT_COL;
        } else {
            titleId = R.string.title_export_chr;
            path = MyFilePickerActivity.DEFAULT_DIR_CHR;
            requestCode = REQUEST_ID_EXPORT_CHR;
        }
        Intent intent = new Intent(this, MyFilePickerActivity.class);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_TITLEID, titleId);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_DIRECTORY, path);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_EXTENSION, MyApplication.FNAMEEXT_PTC);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_WRITEMODE, true);
        startActivityForResult(intent, requestCode);
    }

    private void confirmExportToFile(final int requestCode, final String path) {
        switch (mApp.mEnameModePtc) {
        case MyApplication.ENAME_MODE_EVERY:
            final EditText et = new EditText(this, null, R.attr.editTextEnameStyle);
            OnClickListener cl = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    executeExportToFile(requestCode, path, et.getText().toString());
                }
            };
            Utils.showCustomDialog(this, R.drawable.ic_file, R.string.msg_ename, et, cl);
            break;
        case MyApplication.ENAME_MODE_GUESS:
            executeExportToFile(requestCode, path, guessEmbedName(path));
            break;
        case MyApplication.ENAME_MODE_CONST:
            executeExportToFile(requestCode, path, mApp.mEname);
            break;
        }
    }

    private String guessEmbedName(String path) {
        String fname = MyFilePickerActivity.getFilename(path);
        if (fname.endsWith(MyApplication.FNAMEEXT_PTC)) {
            fname = fname.substring(0, fname.length() - MyApplication.FNAMEEXT_PTC.length());
        }
        StringBuffer buf = new StringBuffer(8);
        for (int i = 0; i < fname.length() && buf.length() < 8; i++) {
            char c = fname.charAt(i);
            if (c >= FULLWIDTH_EXCLAMATION_MARK) c -= (FULLWIDTH_EXCLAMATION_MARK - '!');
            if (c >= 'a') c -= 0x20;
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c == '_') buf.append(c);
        }
        return buf.toString();
    }

    private void executeExportToFile(int requestCode, String path, String ename) {
        boolean ret = false;
        if (ename == null || ename.length() == 0) {
            ename = MyApplication.ENAME_DEFAULT;
        }
        try {
            OutputStream out = new FileOutputStream(path);
            String pname = null;
            if (requestCode == REQUEST_ID_EXPORT_CHR) {
                ret = PTCFile.save(out, ename, PTCFile.PTC_TYPE_CHR, mApp.mChrData.serialize());
                pname = PTCFile.getNameWithType(PTCFile.PTC_TYPE_CHR, ename);
            } else if (requestCode == REQUEST_ID_EXPORT_COL) {
                ret = PTCFile.save(out, ename, PTCFile.PTC_TYPE_COL, mApp.mColData.serialize());
                pname = PTCFile.getNameWithType(PTCFile.PTC_TYPE_COL, ename);
            }
            out.close();
            if (ret) {
                String msg = String.format(getString(R.string.msg_saveptc), pname);
                Utils.showShareDialog(MainActivity.this, R.drawable.ic_export,
                        R.string.menu_export, msg, path);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!ret) {
            Utils.showToast(this, R.string.msg_error);
        }
    }

    private void confirmExportToQRCodes(final int menuId) {
        switch (mApp.mEnameModeQr) {
        case MyApplication.ENAME_MODE_EVERY:
            final EditText et = new EditText(this, null, R.attr.editTextEnameStyle);
            OnClickListener cl = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    executeExportToQRCodes(menuId, et.getText().toString());
                }
            };
            Utils.showCustomDialog(this, R.drawable.ic_file, R.string.msg_ename, et, cl);
            break;
        case MyApplication.ENAME_MODE_CONST:
            executeExportToQRCodes(menuId, mApp.mEname);
            break;
        }
    }

    private void executeExportToQRCodes(int menuId, String ename) {
        boolean ret = false;
        if (ename == null || ename.length() == 0) {
            ename = MyApplication.ENAME_DEFAULT;
        }
        byte[] data;
        String pname;
        if (menuId == R.id.menu_export_qr_chr) {
            data = mApp.mChrData.serialize();
            pname = PTCFile.getNameWithType(PTCFile.PTC_TYPE_CHR, ename);
        } else {
            data = mApp.mColData.serialize();
            pname = PTCFile.getNameWithType(PTCFile.PTC_TYPE_COL, ename);
        }
        String footer = "Generated by ".concat(getString(R.string.app_name))
                .concat("  ").concat(Utils.getVersion(this));
        Bitmap bmp = PTCFile.generateQRCodes(ename, data, mApp.mTightQr, footer);

        if (bmp != null) {
            File dir = new File(MyFilePickerActivity.DEFAULT_DIR_QR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            SimpleDateFormat fmt =
                    new SimpleDateFormat("'qr_'yyMMdd'-'HHmmss'.png'", Locale.US);
            String path = MyFilePickerActivity.DEFAULT_DIR_QR.concat(fmt.format(new Date()));
            try {
                OutputStream out = new FileOutputStream(path);
                bmp.compress(Bitmap.CompressFormat.PNG, 0, out);
                out.close();
                bmp.recycle();
                MediaScannerConnection.scanFile(this,
                        new String[] {path}, new String[] {"image/png"}, null);
                String msg = String.format(getString(R.string.msg_saveqr), pname);
                Utils.showShareDialog(MainActivity.this, R.drawable.ic_export,
                        R.string.menu_export, msg, path);
                ret = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!ret) {
            Utils.showToast(this, R.string.msg_error);
        }
    }

    private void refreshActivity() {
        TabHost tabHost = getTabHost();
        if (!TABTAG_TARGET.equals(tabHost.getCurrentTabTag())) {
            tabHost.setCurrentTabByTag(TABTAG_TARGET);
        } else {
            ChrsActivity activity =
                (ChrsActivity) getLocalActivityManager().getActivity(TABTAG_TARGET);
            activity.drawChrsBitmap();
        }
    }

}