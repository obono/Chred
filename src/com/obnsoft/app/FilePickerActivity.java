/*
 * Copyright (C) 2012 OBN-soft
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

package com.obnsoft.app;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Comparator;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class FilePickerActivity extends ListActivity {

    public static final String INTENT_EXTRA_DIRECTORY = "directory";
    public static final String INTENT_EXTRA_TOPDIRECTORY = "topDirectory";
    public static final String INTENT_EXTRA_EXTENSION = "extension";
    public static final String INTENT_EXTRA_WRITEMODE = "writeMode";
    public static final String INTENT_EXTRA_SELECTPATH = "selectPath";

    private String mDirTop;
    private String mDirCurrent;
    private String mExtension;
    private boolean mWriteMode = false;
    private int mPosNewEntry;
    private ArrayList<String> mStackPath = new ArrayList<String>();
    private FilePickerAdapter mAdapter;

    private int mResIdDir = android.R.drawable.ic_menu_more;
    private int mResIdFile = android.R.drawable.ic_menu_set_as;
    private int mResIdNew = android.R.drawable.ic_menu_add;
    private int mResIdNewMsg = 0;

    class FilePickerAdapter extends ArrayAdapter<File> {

        private Context mContext;

        class FilePickerViewHolder {
            public ImageView imageView;
            public TextView textView;
        }

        public FilePickerAdapter(Context context) {
            super(context, 0);
            mContext = context;
        }

        public void setTargetDirectory(String path) {
            clear();
            File dir = new File(path);
            if (dir != null) {
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        if (file.isHidden()) {
                            return false;
                        }
                        if (mExtension == null || file.isDirectory()) {
                            return true;
                        }
                        return file.getName().toLowerCase().endsWith(mExtension);
                    }
                });
                if (files != null) {
                    for (File file : files) {
                        add(file);
                    }
                    sort(new Comparator<File>() {
                        @Override
                        public int compare(File a, File b) {
                            if (a.isDirectory() != b.isDirectory()) {
                                return (a.isDirectory()) ? -1 : 1;
                            }
                            return a.getName().compareToIgnoreCase(b.getName());
                        }
                    });
                }
            }
            if (mWriteMode) {
                mPosNewEntry = getCount();
                add(null);
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FilePickerViewHolder holder;
            if (convertView == null) {
                LinearLayout ll = new LinearLayout(mContext);
                ll.setGravity(Gravity.CENTER_VERTICAL);
                holder = new FilePickerViewHolder();
                holder.imageView = new ImageView(mContext);
                holder.textView = new TextView(mContext);
                ll.addView(holder.imageView);
                ll.addView(holder.textView);
                ll.setTag(holder);
                convertView = ll;
            } else {
                holder = (FilePickerViewHolder) convertView.getTag();
            }
            File file = (File) getItem(position);
            holder.textView.setSingleLine(true);
            holder.textView.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
            if (mWriteMode && position == mPosNewEntry) {
                holder.textView.setText(
                        (mResIdNewMsg == 0) ? "(New File)" : getText(mResIdNewMsg));
                holder.imageView.setImageResource(mResIdNew);
            } else {
                holder.textView.setText(file.getName());
                holder.imageView.setImageResource(file.isDirectory() ? mResIdDir : mResIdFile);
            }
            return convertView;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String path = null;
        if (intent != null) {
            path = intent.getStringExtra(INTENT_EXTRA_DIRECTORY);
            mDirTop = intent.getStringExtra(INTENT_EXTRA_TOPDIRECTORY);
            mExtension = intent.getStringExtra(INTENT_EXTRA_EXTENSION);
            mWriteMode = intent.getBooleanExtra(INTENT_EXTRA_WRITEMODE, false);
        }

        /*  Check top directory.  */
        if (mDirTop == null) {
            mDirTop = Environment.getExternalStorageDirectory().getPath();
        }
        if (!mDirTop.endsWith(File.separator)) {
            mDirTop += File.separator;
        }

        /*  Check current directory.  */
        if (path == null) {
            path = mDirTop;
        } else {
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
            if (!path.startsWith(mDirTop)) {
                path = mDirTop;
            }
        }

        /*  Check extension.  */
        if (mExtension != null) {
            mExtension = mExtension.toLowerCase();
            if (!mExtension.startsWith(".")) {
                mExtension = "." + mExtension;
            }
        }

        mAdapter = new FilePickerAdapter(this);
        setListAdapter(mAdapter);
        setCurrentDirectory(path);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        File file = (File) mAdapter.getItem(position);
        if (mWriteMode && position == mPosNewEntry) {
            onNewFileRequested(mDirCurrent, mExtension);
        } else if (file.isDirectory()) {
            mStackPath.add(mDirCurrent);
            setCurrentDirectory(file.getPath() + File.separator);
        } else {
            onFileSelected(file.getPath());
        }
    }

    @Override
    public void onBackPressed() {
        String path = getLastDirectory();
        if (path == null) {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        } else {
            mStackPath.remove(mStackPath.size() - 1);
            setCurrentDirectory(path);
        }
    }

    public void setResourceId(int dirId, int fileId, int newId, int newMsgId) {
        if (dirId != 0)     mResIdDir = dirId;
        if (fileId != 0)    mResIdFile = fileId;
        if (newId != 0)     mResIdNew = newId;
        if (newMsgId != 0)  mResIdNewMsg = newMsgId;
    }

    public void setCurrentDirectory(String path) {
        mDirCurrent = path;
        mAdapter.setTargetDirectory(path);
        getListView().smoothScrollBy(0, 0); // Stop momentum scrolling
        onCurrentDirectoryChanged(path);
    }

    public void onCurrentDirectoryChanged(String path) {
        ;
    }

    public void onFileSelected(String path) {
        setResultAndFinish(path);
    }

    public void onNewFileRequested(String directory, String extension) {
        String newPath = directory + "newfile";
        if (extension != null) {
            newPath += extension;
        }
        setResultAndFinish(newPath);
    }

    public void goToUpperDirectory() {
        String path = getUpperDirectory();
        if (path != null) {
            mStackPath.add(mDirCurrent);
            setCurrentDirectory(path);
        }
    }

    public String getTopDirectory() {
        return mDirTop;
    }

    public String getCurrentDirectory() {
        return mDirCurrent;
    }

    public String getExtension() {
        return mExtension;
    }

    public boolean isWriteMode() {
        return mWriteMode;
    }

    public String getTrimmedCurrentDirectory(String path) {
        if (path != null && path.startsWith(mDirTop)) {
            return path.substring(mDirTop.length());
        }
        return null;
    }

    public String getLastDirectory() {
        int size = mStackPath.size();
        return (size > 0) ? mStackPath.get(size - 1) : null;
    }

    public String getUpperDirectory() {
        if (mDirCurrent.equals(mDirTop)) {
            return null;
        }
        int start = mDirCurrent.length() - 1;
        if (mDirCurrent.endsWith(File.separator)) {
            start--;
        }
        int index = mDirCurrent.lastIndexOf(File.separatorChar, start);
        return (index >= 0) ? mDirCurrent.substring(0, index + 1) : null;
    }

    public void setResultAndFinish(String path) {
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_SELECTPATH, path);
        setResult(RESULT_OK, intent);
        finish();
    }
}
