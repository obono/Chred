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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

public class MainActivity extends TabActivity {

    //private static final int REQUEST_ID_CREATE = 1;
    private static final int REQUEST_ID_IMPORT = 2;
    private static final int REQUEST_ID_EXPORT = 3;

    private MyApplication mApp;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mApp = (MyApplication) getApplication();

        TabHost tabHost = getTabHost();
        View view;
        Intent intent;

        intent = new Intent().setClass(this, ChrsActivity.class);
        view = View.inflate(this, R.layout.tab, null);
        ((TextView) view.findViewById(R.id.tab_text)).setText(R.string.target);
        ((ImageView) view.findViewById(R.id.tab_icon)).setImageResource(R.drawable.ic_tab_chr);
        tabHost.addTab(tabHost.newTabSpec("target").setIndicator(view).setContent(intent));

        intent = new Intent().setClass(this, EditActivity.class);
        view = View.inflate(this, R.layout.tab, null);
        ((TextView) view.findViewById(R.id.tab_text)).setText(R.string.edit);
        ((ImageView) view.findViewById(R.id.tab_icon)).setImageResource(R.drawable.ic_tab_edit);
        tabHost.addTab(tabHost.newTabSpec("edit").setIndicator(view).setContent(intent));

        intent = new Intent().setClass(this, PaletteActivity.class);
        view = View.inflate(this, R.layout.tab, null);
        ((TextView) view.findViewById(R.id.tab_text)).setText(R.string.palette);
        ((ImageView) view.findViewById(R.id.tab_icon)).setImageResource(R.drawable.ic_tab_palette);
        tabHost.addTab(tabHost.newTabSpec("palette").setIndicator(view).setContent(intent));

        if (mApp.mCurTab != null) {
            tabHost.setCurrentTabByTag(mApp.mCurTab);
        }
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

    // Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_import_ptc:
            startFilePickerActivityToImport();
            return true;
        case R.id.menu_export_ptc:
            startFilePickerActivityToExport();
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //boolean ret = false;
        switch (requestCode) {
        case REQUEST_ID_IMPORT:
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(MyFilePickerActivity.INTENT_EXTRA_SELECTPATH);
                try {
                    InputStream in = new FileInputStream(path);
                    mApp.mChrData.loadFromStream(in);
                    in.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            break;
        case REQUEST_ID_EXPORT:
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(MyFilePickerActivity.INTENT_EXTRA_SELECTPATH);
                try {
                    OutputStream out = new FileOutputStream(path);
                    mApp.mChrData.saveToStream(out, "ANDROID"/*TODO*/);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            break;
        }
    }

    /*-----------------------------------------------------------------------*/

    private void startFilePickerActivityToImport() {
        final Intent intent = new Intent(this, MyFilePickerActivity.class);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_TITLEID, R.string.app_name/*title_import*/);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_DIRECTORY, MyFilePickerActivity.DEFAULT_DIRECTORY);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_EXTENSION, "ptc");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                startActivityForResult(intent, REQUEST_ID_IMPORT);
            }
        };
        Utils.showYesNoDialog(
                this, android.R.drawable.ic_dialog_alert,
                R.string.menu_import, R.string.msg_newdata, listener);
    }

    private void startFilePickerActivityToExport() {
        Intent intent = new Intent(this, MyFilePickerActivity.class);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_TITLEID, R.string.app_name/*title_export*/);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_DIRECTORY, MyFilePickerActivity.DEFAULT_DIRECTORY);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_EXTENSION, "ptc");
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_WRITEMODE, true);
        startActivityForResult(intent, REQUEST_ID_EXPORT);
    }

}