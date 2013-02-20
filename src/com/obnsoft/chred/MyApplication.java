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
import android.content.res.AssetManager;
import android.util.Log;

public class MyApplication extends Application {

    public int mChrIdx = 0;
    public int mPalIdx = 2;

    public ChrData mChrData;
    public ColData mColData;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate() {
        super.onCreate();

        mChrData = new ChrData();
        mColData = new ColData();
        mChrData.setColData(mColData);

        AssetManager as = getResources().getAssets();
        InputStream in;
        try {
            try {
                in = openFileInput("chara.ptc");
            } catch (FileNotFoundException e) {
                in = as.open("spu1.ptc");
            }
            if (!mChrData.loadFromStream(in)) {
                Log.e("CHRED", "Failed to load character.");
            }
            in.close();
            try {
                in = openFileInput("palette.ptc");
            } catch (FileNotFoundException e) {
                in = as.open("palette.ptc");
            }
            if (!mColData.loadFromStream(in)) {
                Log.e("CHRED", "Failed to load palette.");
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveData() {
        OutputStream out;
        try {
            out = openFileOutput("chara.ptc", MODE_PRIVATE);
            if (!mChrData.saveToStream(out, "android")) {
                Log.e("CHRED", "Failed to save character.");
            }
            out.close();
            out = openFileOutput("palette.ptc", MODE_PRIVATE);
            if (!mColData.saveToStream(out, "android")) {
                Log.e("CHRED", "Failed to save palette.");
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
