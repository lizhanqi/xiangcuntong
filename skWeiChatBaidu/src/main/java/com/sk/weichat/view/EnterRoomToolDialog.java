package com.sk.weichat.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.sk.weichat.R;
import com.sk.weichat.util.ScreenUtil;

public class EnterRoomToolDialog extends Dialog implements View.OnClickListener {
    private EnterRoomToolDialog.OnEnterRoomToolDialog clickListener;
    private Context VContext;

    public EnterRoomToolDialog(@NonNull Context context, EnterRoomToolDialog.OnEnterRoomToolDialog onEnterRoomToolDialog) {
        super(context, R.style.BottomDialog);
        this.VContext = context;
        this.clickListener = onEnterRoomToolDialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_enter_room_tool);
        setCanceledOnTouchOutside(true);
        initView();
    }

    private void initView() {
        ViewGroup root = findViewById(R.id.llRoot);
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof LinearLayout) {
                child.setOnClickListener(this);
            }
        }

        Window o = getWindow();
        WindowManager.LayoutParams lp = o.getAttributes();
        // x/y坐标
        // lp.x = 100;
        // lp.y = 100;
        lp.width = ScreenUtil.getScreenWidth(getContext());
        o.setAttributes(lp);
        this.getWindow().setGravity(Gravity.BOTTOM);
        this.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
    }

    @Override
    public void onClick(View v) {
        dismiss();
        switch (v.getId()) {
            case R.id.llVideo:
                clickListener.numClick();
                break;
            case R.id.llAudio:
                clickListener.inviteClick();
                break;
            case R.id.llCancel:
                clickListener.cancelClick();
                break;

        }
    }

    public interface OnEnterRoomToolDialog {
        void numClick();

        void inviteClick();

        void cancelClick();

    }
}
