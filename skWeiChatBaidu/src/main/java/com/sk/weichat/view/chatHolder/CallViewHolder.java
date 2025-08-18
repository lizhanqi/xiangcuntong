package com.sk.weichat.view.chatHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.util.StringUtils;

class CallViewHolder extends AChatHolderInterface {

    ImageView ivTextImage;
    TextView mTvContent;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_call : R.layout.chat_to_item_call;
    }

    @Override
    public void initView(View view) {
        ivTextImage = view.findViewById(R.id.chat_text_img);
        mTvContent = view.findViewById(R.id.chat_text);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        mTvContent.setText(StringUtils.getAudioMessageContent(message));
        switch (message.getType()) {
            case XmppMessage.TYPE_NO_CONNECT_VOICE:
            case XmppMessage.TYPE_END_CONNECT_VOICE:
                ivTextImage.setImageResource(R.mipmap.end_of_voice_call_icon);
                break;
            case XmppMessage.TYPE_NO_CONNECT_VIDEO:
            case XmppMessage.TYPE_END_CONNECT_VIDEO:
                ivTextImage.setImageResource(R.mipmap.video_call_closed_icon);
                break;
            case XmppMessage.TYPE_NO_CONNECT_SCREEN:
            case XmppMessage.TYPE_END_CONNECT_SCREEN:
                ivTextImage.setImageResource(R.drawable.screen_chat_tool_meeting_icon);
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VOICE:
                ivTextImage.setImageResource(R.mipmap.end_of_voice_call_icon);
            case XmppMessage.TYPE_IS_MU_CONNECT_VIDEO:
                ivTextImage.setImageResource(R.mipmap.video_call_closed_icon);
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_SCREEN:
                ivTextImage.setImageResource(R.drawable.screen_chat_tool_meeting_icon);
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_TALK:
                ivTextImage.setImageResource(R.drawable.talk_chat_tool_icon);
                break;
            case XmppMessage.TYPE_IS_BUSY:
                if (TextUtils.equals(mdata.getObjectId(), String.valueOf(0))) {
                    ivTextImage.setImageResource(R.mipmap.end_of_voice_call_icon);
                } else {
                    ivTextImage.setImageResource(R.mipmap.video_call_closed_icon);
                }
                break;
        }
    }

    @Override
    protected void onRootClick(View v) {

    }

    /**
     * 重写该方法，return true 表示自动发送已读
     *
     * @return
     */
    @Override
    public boolean enableSendRead() {
        return true;
    }
}
