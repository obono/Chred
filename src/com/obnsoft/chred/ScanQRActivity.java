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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ScanQRActivity extends Activity {

    public static final String INTENT_EXTRA_DATA = "data";
    public static final int RESULT_FAILED = RESULT_FIRST_USER;

    protected static final int COLOR_DEFAULT = Color.argb(96, 255, 0, 0);
    protected static final int COLOR_FAIL = Color.rgb(192, 192, 0);
    protected static final int COLOR_SUCCESS = Color.rgb(0, 128, 255);

    private TextView mTextMsg;
    private TextView mTextInfo;
    private GradientDrawable mQrFrameDrawable;
    private int mQrFrameSize;

    protected View mQrFrame;
    protected ScanQRManager mQRMan;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextMsg = (TextView) findViewById(R.id.text_qrmsg);
        mTextInfo = (TextView) findViewById(R.id.text_qrinfo);
        mQrFrame = (View) findViewById(R.id.view_qrframe);
        mQrFrameDrawable = new GradientDrawable();
        mQrFrameDrawable.setColor(Color.TRANSPARENT);
        mQrFrameSize = Utils.dp2px(this, 4);
        mQrFrame.setBackgroundDrawable(mQrFrameDrawable);
        setFrameDefaultColor();

        mQRMan = new ScanQRManager(this);
        setInformation();
    }

    protected void setInformation() {
        int total = mQRMan.getTotalQRNumber();
        int current = mQRMan.getCurrentQRNumber();
        if (total == 0 || current < total) {
            mTextMsg.setText(String.format(getString(R.string.qr_msg), current + 1));
        }
        if (total > 0) {
            mTextInfo.setText(String.format(
                    "%s(%d/%d)", mQRMan.getNameWithType(), current, total));
        }
    }

    protected void setFrameDefaultColor() {
        mQrFrameDrawable.setStroke(mQrFrameSize, COLOR_DEFAULT);
    }

    protected void setFrameResultColor(boolean isSuccess) {
        mQrFrameDrawable.setStroke(mQrFrameSize, isSuccess ? COLOR_SUCCESS : COLOR_FAIL);
    }

    protected void setSuccessResult(byte[] data) {
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_DATA, data);
        setResult(RESULT_OK, intent);
        finish();
    }

    protected void setFailedResult() {
        Intent intent = new Intent();
        setResult(RESULT_FAILED, intent);
        finish();
    }

    protected void setCanceledResult() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

}
