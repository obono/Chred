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
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
    //private static final int REQUEST_ID_IMPORT_GALLERY = 2;
    //private static final int REQUEST_ID_IMPORT_CAMERA = 3;

    private static final int REQUEST_ID_EXPORT_CHR = 11;
    private static final int REQUEST_ID_EXPORT_COL = 12;
    //private static final int REQUEST_ID_EXPORT_QR = 13;

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
        case R.id.menu_import_preset:
            selectPresetToImport();
            return true;
        case R.id.menu_export_chr:
        case R.id.menu_export_col:
            requestFileToExport(menuId);
            return true;
        case R.id.menu_export_qr_chr:
        case R.id.menu_export_qr_col:
            executeExportToQRCodes(menuId);
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
        case REQUEST_ID_EXPORT_CHR:
        case REQUEST_ID_EXPORT_COL:
            if (resultCode == RESULT_OK) {
                executeExportToFile(requestCode,
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
            executeImportFromStream(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void selectPresetToImport() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_import_preset)
                .setItems(PRESET_FNAMES, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String fname;
                        if (which == 0) {
                            fname = "palette.ptc";
                        } else {
                            fname = PRESET_FNAMES[which].toLowerCase(Locale.US)
                                    .concat(MyApplication.FNAMEEXT_PTC);
                        }
                        try {
                            executeImportFromStream(getResources().getAssets().open(fname));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).show();
    }

    private void executeImportFromStream(InputStream in) {
        int msgId = R.string.msg_error;
        try {
            PTCFile ptcfile = new PTCFile();
            if (ptcfile.load(in)) {
                if (ptcfile.getType() == PTCFile.PTC_TYPE_CHR) {
                    mApp.mChrData.deserialize(ptcfile.getData());
                    msgId = R.string.msg_loadchr;
                    refreshActivity();
                } else if (ptcfile.getType() == PTCFile.PTC_TYPE_COL) {
                    mApp.mColData.deserialize(ptcfile.getData());
                    msgId = R.string.msg_loadcol;
                    refreshActivity();
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.showToast(this, msgId);
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

    private void executeExportToFile(int requestCode, String path) {
        boolean ret = false;
        try {
            int msgId = R.string.msg_error;
            String strName = MyApplication.PTC_KEYWORD; // TODO
            OutputStream out = new FileOutputStream(path);
            if (requestCode == REQUEST_ID_EXPORT_CHR) {
                ret = PTCFile.save(out, strName, PTCFile.PTC_TYPE_CHR, mApp.mChrData.serialize());
                msgId = R.string.msg_savechr;
            } else if (requestCode == REQUEST_ID_EXPORT_COL) {
                ret = PTCFile.save(out, strName, PTCFile.PTC_TYPE_COL, mApp.mColData.serialize());
                msgId = R.string.msg_savecol;
            }
            out.close();
            if (ret) {
                Utils.showShareDialog(MainActivity.this, R.drawable.ic_export,
                        R.string.menu_export, msgId, path);
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

    private void executeExportToQRCodes(int menuId) {
        byte[] data = (menuId == R.id.menu_export_qr_chr) ?
                mApp.mChrData.serialize() : mApp.mColData.serialize();
        Bitmap bmp = PTCFile.generateQRCodes(MyApplication.PTC_KEYWORD, data); // TODO
        if (bmp != null) {
            try {
                File dir = new File(MyFilePickerActivity.DEFAULT_DIR_QR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                SimpleDateFormat fmt =
                        new SimpleDateFormat("'qr_'yyMMdd'-'HHmmss'.png'", Locale.US);
                String path = MyFilePickerActivity.DEFAULT_DIR_QR.concat(fmt.format(new Date()));
                OutputStream out;
                out = new FileOutputStream(path);
                bmp.compress(Bitmap.CompressFormat.PNG, 0, out);
                out.close();
                bmp.recycle();
                MediaScannerConnection.scanFile(this,
                        new String[] {path}, new String[] {"image/png"}, null);
                Utils.showShareDialog(MainActivity.this, R.drawable.ic_export,
                        R.string.menu_export, R.string.msg_saveqr, path);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
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