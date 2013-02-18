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
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class MainActivity extends TabActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Resources res = getResources();
        TabHost tabHost = getTabHost();
        TabHost.TabSpec spec;
        Intent intent;
        intent = new Intent().setClass(this, ChrsActivity.class);
        spec = tabHost.newTabSpec("tab1")
                      .setIndicator("Target",
                       res.getDrawable(android.R.drawable.ic_menu_add))
                      .setContent(intent);
        tabHost.addTab(spec);
        intent = new Intent().setClass(this, EditActivity.class);
        spec = tabHost.newTabSpec("tab2")
                      .setIndicator("Edit",
                       res.getDrawable(android.R.drawable.ic_menu_add))
                      .setContent(intent);
        tabHost.addTab(spec);
        intent = new Intent().setClass(this, PaletteActivity.class);
        spec = tabHost.newTabSpec("tab3")
                      .setIndicator("Palette",
                       res.getDrawable(android.R.drawable.ic_menu_add))
                      .setContent(intent);
        tabHost.addTab(spec);
        tabHost.setCurrentTab(1);
    }

}