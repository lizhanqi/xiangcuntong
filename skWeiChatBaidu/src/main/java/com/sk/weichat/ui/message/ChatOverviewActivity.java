package com.sk.weichat.ui.message;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import com.alibaba.fastjson.JSON;
import com.example.qrcode.utils.DecodeUtils;
import com.google.zxing.Result;
import com.sk.weichat.R;
import com.sk.weichat.adapter.ChatOverviewAdapter;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.helper.ImageLoadHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.BitmapUtil;
import com.sk.weichat.util.FileUtil;
import com.sk.weichat.view.SaveWindow;
import com.sk.weichat.view.imageedit.IMGEditActivity;

import java.util.List;

public class ChatOverviewActivity extends BaseActivity {
    public static final int REQUEST_IMAGE_EDIT = 1;
    public static String imageChatMessageListStr;
    private ViewPager mViewPager;
    private ChatOverviewAdapter mChatOverviewAdapter;
    private List<ChatMessage> mChatMessages;
    private int mFirstShowPosition;
    private String mCurrentShowUrl;
    private String mEditedPath;
    private SaveWindow mSaveWindow;
    private My_BroadcastReceivers my_broadcastReceiver = new My_BroadcastReceivers();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_overview);
        // imageChatMessageListStr = getIntent().getStringExtra("imageChatMessageList");
        mChatMessages = JSON.parseArray(imageChatMessageListStr, ChatMessage.class);
        imageChatMessageListStr = "";
        if (mChatMessages == null) {
            finish();
            return;
        }
        mFirstShowPosition = getIntent().getIntExtra("imageChatMessageList_current_position", 0);
        getCurrentShowUrl(mFirstShowPosition);

        initView();
        register();
    }

    @Override
    protected void onDestroy() {
        if (my_broadcastReceiver != null) {
            try {
                unregisterReceiver(my_broadcastReceiver);
            } catch (Exception e) {
                // 以防万一，
            }
        }
        super.onDestroy();
    }

    private void initView() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        mViewPager = findViewById(R.id.chat_overview_vp);
        mChatOverviewAdapter = new ChatOverviewAdapter(this, mChatMessages);
        mViewPager.setAdapter(mChatOverviewAdapter);
        mViewPager.setCurrentItem(mFirstShowPosition);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int arg0) {
                getCurrentShowUrl(arg0);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
    }

    private void getCurrentShowUrl(int position) {
        if (position >= mChatMessages.size()) {
            // 以防万一，静态变量可能导致各种无法预料的崩溃，
            return;
        }
        ChatMessage chatMessage = mChatMessages.get(position);
        if (!TextUtils.isEmpty(chatMessage.getFilePath()) && FileUtil.isExist(chatMessage.getFilePath())) {
            mCurrentShowUrl = chatMessage.getFilePath();
        } else {
            mCurrentShowUrl = chatMessage.getContent();
        }
    }

    private void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(com.sk.weichat.broadcast.OtherBroadcast.singledown);
        filter.addAction(com.sk.weichat.broadcast.OtherBroadcast.longpress);
        registerReceiver(my_broadcastReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_EDIT:
                    mCurrentShowUrl = mEditedPath;
                    ChatMessage chatMessage = mChatMessages.get(mViewPager.getCurrentItem());
                    chatMessage.setFilePath(mCurrentShowUrl);
                    mChatMessages.set(mViewPager.getCurrentItem(), chatMessage);
                    mChatOverviewAdapter.refreshItem(mCurrentShowUrl, mViewPager.getCurrentItem());
                    // 模拟那个长按，弹出菜单，
                    Intent intent = new Intent(com.sk.weichat.broadcast.OtherBroadcast.longpress);
                    sendBroadcast(intent);
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class My_BroadcastReceivers extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(com.sk.weichat.broadcast.OtherBroadcast.singledown)) {
                finish();
            } else if (intent.getAction().equals(com.sk.weichat.broadcast.OtherBroadcast.longpress)) {
                // 长按屏幕，弹出菜单
                if (mCurrentShowUrl.contains("http")) {
                    ImageLoadHelper.loadBitmapDontAnimate(mContext, mCurrentShowUrl, b -> {
                        Bitmap bitmap = BitmapUtil.getBitmapQRCode(ChatOverviewActivity.this, b);
                        mSaveWindow = new SaveWindow(ChatOverviewActivity.this,
                                bitmap != null, new ClickListener(bitmap));
                        mSaveWindow.show();
                    }, e -> {
                        // todo load bitmap failed
                    });
                } else {
                    Bitmap bitmap = BitmapUtil.getBitmapQRCode(ChatOverviewActivity.this, mCurrentShowUrl);
                    mSaveWindow = new SaveWindow(ChatOverviewActivity.this,
                            bitmap != null, new ClickListener(bitmap));
                    mSaveWindow.show();
                }
            }
        }
    }

    class ClickListener implements View.OnClickListener {

        private Bitmap bitmap;

        public ClickListener(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public void onClick(View v) {
            mSaveWindow.dismiss();
            int viewId = v.getId();
            if (viewId == R.id.save_image) {
                FileUtil.downImageToGallery(ChatOverviewActivity.this, mCurrentShowUrl);
            } else if (viewId == R.id.edit_image) {
                ImageLoadHelper.loadFile(
                        ChatOverviewActivity.this,
                        mCurrentShowUrl,
                        f -> {
                            mEditedPath = FileUtil.createImageFileForEdit().getAbsolutePath();
                            IMGEditActivity.startForResult(ChatOverviewActivity.this, Uri.fromFile(f), mEditedPath, REQUEST_IMAGE_EDIT);
                        });
            } else if (viewId == R.id.identification_qr_code) {
                // 识别图中二维码
                if (bitmap == null) {// 理论上不太可能了，因为该item显示时，bitmap都不为空
                    Toast.makeText(ChatOverviewActivity.this, R.string.unrecognized, Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    final Result result = DecodeUtils.decodeFromPicture(bitmap);
                    mViewPager.post(() -> {
                        if (result != null && !TextUtils.isEmpty(result.getText())) {
                            HandleQRCodeScanUtil.handleScanResult(mContext, result.getText());
                        } else {
                            Toast.makeText(ChatOverviewActivity.this, R.string.decode_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            }

        }
    }
}
