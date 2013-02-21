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

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

public class MainActivity extends TabActivity {

    MyApplication mApp;

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
        ((ImageView) view.findViewById(R.id.tab_icon)).setImageResource(R.drawable.icon);
        tabHost.addTab(tabHost.newTabSpec("target").setIndicator(view).setContent(intent));

        intent = new Intent().setClass(this, EditActivity.class);
        view = View.inflate(this, R.layout.tab, null);
        ((TextView) view.findViewById(R.id.tab_text)).setText(R.string.edit);
        ((ImageView) view.findViewById(R.id.tab_icon)).setImageResource(R.drawable.icon);
        tabHost.addTab(tabHost.newTabSpec("edit").setIndicator(view).setContent(intent));

        intent = new Intent().setClass(this, PaletteActivity.class);
        view = View.inflate(this, R.layout.tab, null);
        ((TextView) view.findViewById(R.id.tab_text)).setText(R.string.palette);
        ((ImageView) view.findViewById(R.id.tab_icon)).setImageResource(R.drawable.icon);
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

}