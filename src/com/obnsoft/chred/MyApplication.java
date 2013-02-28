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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyApplication extends Application {

    public static final String PTC_KEYWORD = "ANDROID";
    public static final String FNAMEEXT_PTC = ".ptc";
    public static final String FNAME_DEFAULT_CHR = "chara.ptc";
    public static final String FNAME_DEFAULT_COL = "palette.ptc";

    public int mChrIdx;
    public int mPalIdx;
    public int mColIdx;
    public String mCurTab;

    public ChrData mChrData;
    public ColData mColData;
    public PaletteAdapter mPalAdapter;

    private static final String TAG = "CHRED";

    private static final String PREF_KEY_CHR = "chara";
    private static final String PREF_KEY_PAL = "palette";
    private static final String PREF_KEY_COL = "color";
    private static final String PREF_KEY_TAB = "tab";
    private static final String PREF_KEY_HUNITS = "h_units";
    private static final String PREF_KEY_VUNITS = "v_units";

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate() {
        super.onCreate();

        mChrData = new ChrData();
        mColData = new ColData();
        mChrData.setColData(mColData);
        mPalAdapter = new PaletteAdapter(this, mColData);

        AssetManager as = getResources().getAssets();
        InputStream in;
        try {
            try {
                in = openFileInput(FNAME_DEFAULT_CHR);
            } catch (FileNotFoundException e) {
                in = as.open("spu1.ptc");
            }
            if (!mChrData.loadFromStream(in)) {
                Log.e(TAG, "Failed to load character.");
            }
            in.close();
            try {
                in = openFileInput(FNAME_DEFAULT_COL);
            } catch (FileNotFoundException e) {
                in = as.open("palette.ptc");
            }
            if (!mColData.loadFromStream(in)) {
                Log.e(TAG, "Failed to load palette.");
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mChrData.resetDirty();
        mColData.resetDirty();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mChrIdx = prefs.getInt(PREF_KEY_CHR, 0);
        mPalIdx = prefs.getInt(PREF_KEY_PAL, 2);
        mColIdx = prefs.getInt(PREF_KEY_COL, 0);
        mCurTab = prefs.getString(PREF_KEY_TAB, null);
        mChrData.setTargetSize(prefs.getInt(PREF_KEY_HUNITS, 2), prefs.getInt(PREF_KEY_VUNITS, 2));
    }

    public void saveData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_CHR, mChrIdx);
        editor.putInt(PREF_KEY_PAL, mPalIdx);
        editor.putInt(PREF_KEY_COL, mColIdx);
        editor.putString(PREF_KEY_TAB, mCurTab);
        editor.putInt(PREF_KEY_HUNITS, mChrData.getTargetSizeH());
        editor.putInt(PREF_KEY_VUNITS, mChrData.getTargetSizeV());
        editor.commit();

        OutputStream out;
        try {
            if (mChrData.getDirty()) {
                out = openFileOutput(FNAME_DEFAULT_CHR, MODE_PRIVATE);
                if (!mChrData.saveToStream(out, PTC_KEYWORD)) {
                    Log.e(TAG, "Failed to save character.");
                }
                out.close();
                mChrData.resetDirty();
            }
            if (mColData.getDirty()) {
                out = openFileOutput(FNAME_DEFAULT_COL, MODE_PRIVATE);
                if (!mColData.saveToStream(out, PTC_KEYWORD)) {
                    Log.e(TAG, "Failed to save palette.");
                }
                out.close();
                mColData.resetDirty();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
