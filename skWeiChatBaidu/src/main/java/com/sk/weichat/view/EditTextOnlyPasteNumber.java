package com.sk.weichat.view;

import android.content.ClipboardManager;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatEditText;

public class EditTextOnlyPasteNumber extends AppCompatEditText {
    private static final String TAG = "EditTextOnlyPasteNumber";

    public EditTextOnlyPasteNumber(Context context) {
        super(context);
    }

    public EditTextOnlyPasteNumber(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextOnlyPasteNumber(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.paste) {
            //调用剪贴板
            ClipboardManager clip = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            //改变剪贴板中Content
            if (clip != null) {
                Log.e(TAG, "剪贴板中的内容-->" + clip.getText());
            }
        }
        return super.onTextContextMenuItem(id);
    }
}