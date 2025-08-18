package com.sk.weichat.view.chatHolder;

import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.db.dao.ChatMessageDao;

public class DiceViewHolder extends AChatHolderInterface {

    ImageView mImageView;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_dice : R.layout.chat_to_item_dice;
    }

    @Override
    public void initView(View view) {
        mImageView = view.findViewById(R.id.chat_image);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        if (!message.isDownload()) {
            mImageView.setBackgroundResource(R.drawable.dice_frame);
            AnimationDrawable animationDrawable = (AnimationDrawable) mImageView.getBackground();
            if (animationDrawable.isRunning()) {
                animationDrawable.stop();
            }
            animationDrawable.start();
            new Handler().postDelayed(() -> {
                message.setDownload(true);
                drawableDice(message.getContent());
            }, 2000);
        } else {
            drawableDice(message.getContent());
        }
        if (isGounp) {
            ChatMessageDao.getInstance().updateMessageAnimationState(mLoginUserId, message.getToUserId(), message.getPacketId());
        } else {
            if (message.isMySend()) {
                ChatMessageDao.getInstance().updateMessageAnimationState(mLoginUserId, message.getToUserId(), message.getPacketId());
            } else {
                ChatMessageDao.getInstance().updateMessageAnimationState(mLoginUserId, message.getFromUserId(), message.getPacketId());
            }
        }
    }

    @Override
    protected void onRootClick(View v) {
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    private void drawableDice(String message) {
        // 删除与撤回都会影响message，需要判断
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (message.contains(".")) {
            int strOf = message.lastIndexOf(".");
            message = message.substring(0, strOf);
        }
        int value;
        try {
            value = Integer.valueOf(message);
        } catch (Exception e) {
            return;
        }
        int id = MyApplication.getContext().getResources().getIdentifier("dice_" + value,
                "drawable", MyApplication.getContext().getPackageName());
        mImageView.setBackgroundResource(id);
    }
}
