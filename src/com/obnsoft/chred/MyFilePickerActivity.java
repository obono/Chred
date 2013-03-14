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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.obnsoft.app.FilePickerActivity;

public class MyFilePickerActivity extends FilePickerActivity {

    public static final String INTENT_EXTRA_TITLEID = "titleId";
    public static final String DEFAULT_DIR =
        Environment.getExternalStorageDirectory().getPath().concat("/petitcom/");
    public static final String DEFAULT_DIR_COL = DEFAULT_DIR.concat("COL/");
    public static final String DEFAULT_DIR_CHR = DEFAULT_DIR.concat("CHR/");
    public static final String DEFAULT_DIR_QR = DEFAULT_DIR.concat("QR/");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.file_picker);
        Intent intent = getIntent();
        if (intent != null) {
            int id = intent.getIntExtra(INTENT_EXTRA_TITLEID, 0);
            if (id != 0) {
                setTitle(id);
            }
            String path = intent.getStringExtra(INTENT_EXTRA_DIRECTORY);
            File dir = new File((path == null) ? DEFAULT_DIR : path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        super.onCreate(savedInstanceState);
        setResourceId(R.drawable.ic_folder, R.drawable.ic_file,
                R.drawable.ic_newfile, R.string.msg_createfile);
    }

    @Override
    public void onCurrentDirectoryChanged(String path) {
        super.onCurrentDirectoryChanged(path);
        TextView tv = (TextView) findViewById(R.id.text_current_directory);
        tv.setText(getTrimmedCurrentDirectory(path));
        ImageButton btn = (ImageButton) findViewById(R.id.button_back_directory);
        btn.setEnabled(getLastDirectory() != null);
        btn = (ImageButton) findViewById(R.id.button_upper_directory);
        btn.setEnabled(getUpperDirectory() != null);
    }

    @Override
    public void onFileSelected(final String path) {
        if (isWriteMode()) {
            DialogInterface.OnClickListener cl = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    setResultAndFinish(path);
                }
            };
            String msg = String.format(getString(R.string.msg_overwrite), getFilename(path));
            Utils.showYesNoDialog(this, R.drawable.ic_caution,
                    R.string.menu_export, msg, cl);
        } else {
            setResultAndFinish(path);
        }
    }

    @Override
    public void onNewFileRequested(final String directory, final String extension) {
        final EditText editText = new EditText(this);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String fileName = Utils.trimUni(editText.getText().toString());
                if (fileName.length() == 0 || fileName.startsWith(".") ||
                        fileName.contains(File.separator) || fileName.contains(File.pathSeparator)) {
                    Utils.showToast(MyFilePickerActivity.this, R.string.msg_invalid);
                    return;
                }
                String newPath = directory.concat(fileName);
                if (extension != null && !newPath.endsWith(extension)) {
                    newPath += extension;
                }
                if ((new File(newPath)).exists()) {
                    onFileSelected(newPath);
                } else {
                    setResultAndFinish(newPath);
                }
            }
        };
        Utils.showCustomDialog(this, R.drawable.ic_newfile,
                R.string.msg_newfilename, editText, listener);
    }

    public void onBackDirectory(View v) {
        onBackPressed();
    }

    public void onUpperDirectory(View v) {
        goToUpperDirectory();
    }

    public static String getFilename(String path) {
        return path.substring(path.lastIndexOf(File.separatorChar) + 1);
    }

}
