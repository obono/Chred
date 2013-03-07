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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
        case R.id.menu_version:
            showVersion();
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
        int msgId = R.string.msg_error;
        try {
            PTCFile ptcfile = new PTCFile();
            InputStream in = new FileInputStream(path);
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.showToast(this, msgId);
    }

    private void selectPresetToImport() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_import_preset)
                .setItems(PRESET_FNAMES, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            executeImportFromPreset("PALETTE");
                        } else {
                            executeImportFromPreset(PRESET_FNAMES[which]);
                        }
                    }
                }).show();
    }

    private void executeImportFromPreset(String name) {
        int msgId = R.string.msg_error;
        try {
            PTCFile ptcfile = new PTCFile();
            InputStream in = getResources().getAssets().open(
                    name.toLowerCase().concat(MyApplication.FNAMEEXT_PTC));
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
        int msgId = R.string.msg_error;
        try {
            String strName = MyApplication.PTC_KEYWORD; // TODO
            OutputStream out = new FileOutputStream(path);
            if (requestCode == REQUEST_ID_EXPORT_CHR) {
                if (PTCFile.save(out, strName, PTCFile.PTC_TYPE_CHR, mApp.mChrData.serialize())) {
                    msgId = R.string.msg_savechr;
                }
            } else {
                if (PTCFile.save(out, strName, PTCFile.PTC_TYPE_COL, mApp.mColData.serialize())) {
                    msgId = R.string.msg_savecol;
                }
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.showToast(this, msgId);
    }

    private void showVersion() {
        final View aboutView = View.inflate(this, R.layout.about, null);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            TextView textView = (TextView) aboutView.findViewById(R.id.text_about_version);
            textView.setText("Version ".concat(packageInfo.versionName));

            StringBuilder buf = new StringBuilder();
            InputStream in = getResources().openRawResource(R.raw.license);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String str;
            while((str = reader.readLine()) != null) {
                buf.append(str).append('\n');
            }
            textView = (TextView) aboutView.findViewById(R.id.text_about_message);
            textView.setText(buf.toString());
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.showCustomDialog(this, R.drawable.ic_version, R.string.menu_version, aboutView, null);
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