package com.sk.weichat.ui.message.single;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.Label;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.LabelDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.SkinUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.ViewHolder;
import com.sk.weichat.view.MyListView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class SetRemarkActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "SetRemarkActivity";
    private EditText mRemarkNameEdit;
    private TextView tv_setting_label;
    private EditText etDescribe;
    private MyListView lvAddTelephone;
    private AddTelephoneAdapter mAddTelephoneAdapter;

    private String mLoginUserId;
    private String mFriendId;
    @Nullable
    private Friend mFriend;
    private String name, desc, telephone;
    private List<String> data = new ArrayList<>();
    private String originalLabelName;

    private int mCurrentInputPosition = -1;

    private boolean isSetLabelResult, isSetRemarkResult, isSetTelephoneResult;
    private boolean isNeedReady, isReady;// 如满足isSetRemarkResult, isSetTelephoneResult均为true的条件，则需要标记isNeedReady为true，并且isReady为true才可以结束当前界面

    public static void start(Context ctx, String friendId, String telephone) {
        Intent intent = new Intent(ctx, SetRemarkActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friendId);
        intent.putExtra("telephone", telephone);
        ctx.startActivity(intent);
    }

    public static void startForResult(Activity ctx, String friendId, String name, String desc, String telephone, int requestCode) {
        Intent intent = new Intent(ctx, SetRemarkActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friendId);
        intent.putExtra("name", name);
        intent.putExtra("desc", desc);
        intent.putExtra("telephone", telephone);
        ctx.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_remark);
        mFriendId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        // 现在还可以针对陌生人设置备注与描述
        name = getIntent().getStringExtra("name");
        desc = getIntent().getStringExtra("desc");
        telephone = getIntent().getStringExtra("telephone");
        if (TextUtils.isEmpty(telephone)) {
            data.add("");
        } else {
            String[] split = telephone.split(";");
            List<String> list = Arrays.asList(split);
            data = new ArrayList<>(list);
            data.add(data.size(), "");// 添加一个输入框
        }

        mLoginUserId = coreManager.getSelf().getUserId();
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);
        initActionBar();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLabel();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(this);
        TextView tvTitle = findViewById(R.id.tv_title_center);
        if (mFriend == null) {
            tvTitle.setText(R.string.tip_set_remark);
        } else {
            tvTitle.setText(R.string.set_remark_and_label);
        }
        TextView tvRight = findViewById(R.id.tv_title_right);
        tvRight.setTextColor(getResources().getColor(R.color.white));
        tvRight.setBackground(mContext.getResources().getDrawable(R.drawable.bg_btn_grey_circle));
        ViewCompat.setBackgroundTintList(tvRight, ColorStateList.valueOf(SkinUtils.getSkin(this).getAccentColor()));
        tvRight.setText(R.string.finish);
        tvRight.setOnClickListener(this);
    }

    private void initView() {
        mRemarkNameEdit = findViewById(R.id.department_edit);
        if (!TextUtils.isEmpty(currentRemarkName())) {
            mRemarkNameEdit.setText(currentRemarkName());
        }

        tv_setting_label = findViewById(R.id.tv_setting_label);
        if (mFriend == null) {
            findViewById(R.id.ll1).setVisibility(View.GONE);
        }
        loadLabel();
        originalLabelName = tv_setting_label.getText().toString();
        findViewById(R.id.rlLabel).setOnClickListener(v -> {
            Intent intentLabel = new Intent(this, SetLabelActivity.class);
            intentLabel.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
            startActivity(intentLabel);
        });

        etDescribe = findViewById(R.id.etDescribe);
        if (!TextUtils.isEmpty(currentDescribe())) {
            etDescribe.setText(currentDescribe());
        }

        lvAddTelephone = findViewById(R.id.lvAddTelephone);
        mAddTelephoneAdapter = new AddTelephoneAdapter(this);
        lvAddTelephone.setAdapter(mAddTelephoneAdapter);
    }

    private void loadLabel() {
        List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mFriendId);
        String labelNames = "";
        if (friendLabelList != null && friendLabelList.size() > 0) {
            for (int i = 0; i < friendLabelList.size(); i++) {
                if (i == friendLabelList.size() - 1) {
                    labelNames += friendLabelList.get(i).getGroupName();
                } else {
                    labelNames += friendLabelList.get(i).getGroupName() + ",";
                }
            }
            tv_setting_label.setText(labelNames);
            tv_setting_label.setTextColor(getResources().getColor(R.color.hint_text_color));
        } else {
            tv_setting_label.setText(getResources().getString(R.string.remark_tag));
            tv_setting_label.setTextColor(getResources().getColor(R.color.hint_text_color));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_title_left:
                finish();
                break;
            case R.id.tv_title_right:
                if (!TextUtils.equals(originalLabelName, tv_setting_label.getText().toString())) {
                    // 标签改变了
                    Log.e(TAG, "标签改变了");
                    Log.e(TAG, "originalLabelName：" + originalLabelName);
                    Log.e(TAG, "tv_setting_label.getText().toString()：" + tv_setting_label.getText().toString());
                    isSetLabelResult = true;
                }

                String remarkName = mRemarkNameEdit.getText().toString().trim();
                String describe = etDescribe.getText().toString().trim();
                if (!TextUtils.equals(currentRemarkName(), remarkName) ||
                        !TextUtils.equals(currentDescribe(), describe)) {
                    // 备注 || 描述改变了，调接口更新
                    Log.e(TAG, "备注 || 描述改变了，调接口更新");
                    Log.e(TAG, "currentRemarkName：" + currentRemarkName());
                    Log.e(TAG, "remarkName：" + remarkName);
                    Log.e(TAG, "currentDescribe：" + currentDescribe());
                    Log.e(TAG, "describe：" + describe);
                    isSetRemarkResult = true;
                }

                for (int i = 0; i < data.size(); i++) {
                    List<String> dataEmpty = new ArrayList<>();
                    if (TextUtils.isEmpty(data.get(i))) {
                        dataEmpty.add(data.get(i));
                    }
                    data.removeAll(dataEmpty);
                }
                StringBuilder remarkTelephone = new StringBuilder();
                for (int i = 0; i < data.size(); i++) {
                    if (i != data.size() - 1) {
                        remarkTelephone.append(data.get(i)).append(";");
                    } else {
                        remarkTelephone.append(data.get(i));
                    }
                }
                if (!TextUtils.equals(telephone, remarkTelephone.toString())) {
                    // 电话号码改变了，调接口更新
                    Log.e(TAG, "电话号码改变了，调接口更新");
                    Log.e(TAG, "telephone：" + telephone);
                    Log.e(TAG, "remarkTelephone：" + remarkTelephone);
                    isSetTelephoneResult = true;
                }

                if (!isSetLabelResult && !isSetRemarkResult && !isSetTelephoneResult) {
                    // 都无更新
                    Log.e(TAG, "都无更新");
                    finish();
                    return;
                } else if (isSetLabelResult && !isSetRemarkResult && !isSetTelephoneResult) {
                    // 仅更新了标签
                    Log.e(TAG, "仅更新了标签");
                    setResult(RESULT_OK);
                    finish();
                    return;
                } else if (isSetRemarkResult && isSetTelephoneResult) {
                    Log.e(TAG, "备注 || 描述 与电话号码都更新了");
                    isNeedReady = true;
                    remarkFriend(remarkName, describe);
                    remarkFriend(remarkTelephone.toString());
                } else if (isSetRemarkResult) {
                    Log.e(TAG, "仅更新了备注 || 描述");
                    remarkFriend(remarkName, describe);
                } else {
                    Log.e(TAG, "仅更新电话号码");
                    remarkFriend(remarkTelephone.toString());
                }
                break;
        }
    }

    @Nullable
    private String currentRemarkName() {
        if (mFriend == null) {
            if (name == null) {
                name = "";
            }
            return name;
        }
        return mFriend.getRemarkName();
    }

    @Nullable
    private String currentDescribe() {
        if (mFriend == null) {
            if (desc == null) {
                desc = "";
            }
            return desc;
        }
        return mFriend.getDescribe();
    }

    private void remarkFriend(String remarkName, String describe) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        params.put("remarkName", remarkName);
        params.put("describe", describe);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_REMARK)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            FriendDao.getInstance().updateRemarkNameAndDescribe(
                                    mLoginUserId, mFriendId, remarkName, describe);

                            MsgBroadcast.broadcastMsgUiUpdate(mContext);
                            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                            Intent intent = new Intent(com.sk.weichat.broadcast.OtherBroadcast.NAME_CHANGE);
                            intent.putExtra("remarkName", remarkName);
                            intent.putExtra("describe", describe);
                            sendBroadcast(intent);

                            if (isNeedReady) {
                                if (isReady) {
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            } else {
                                setResult(RESULT_OK);
                                finish();
                            }
                            isReady = true;
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    private void remarkFriend(String phoneRemark) {
        if (TextUtils.isEmpty(phoneRemark)) {
            phoneRemark = "";
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        params.put("phoneRemark", phoneRemark);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_REMARK_PHONE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            ToastUtil.showToast(mContext, getString(R.string.success));
                            if (isNeedReady) {
                                if (isReady) {
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            } else {
                                setResult(RESULT_OK);
                                finish();
                            }
                            isReady = true;
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    class AddTelephoneAdapter extends BaseAdapter {
        private Context mContext;
        private EditText mCurrentEditText;

        public AddTelephoneAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.row_add_telephone, viewGroup, false);
            }
            EditText etInputTelephone = ViewHolder.get(view, R.id.etInputTelephone);
            ImageView ivClear = ViewHolder.get(view, R.id.ivClear);
            View vLine = ViewHolder.get(view, R.id.vLine);

            if (etInputTelephone.getTag() instanceof TextWatcher) {
                etInputTelephone.removeTextChangedListener((TextWatcher) etInputTelephone.getTag());
            }

            etInputTelephone.setText(data.get(i));
            ivClear.setVisibility(TextUtils.isEmpty(data.get(i)) ? View.GONE : View.VISIBLE);
            vLine.setVisibility(i == data.size() - 1 ? View.GONE : View.VISIBLE);

            ivClear.setOnClickListener(v -> {
                data.set(i, "");
                notifyDataSetChanged();
            });

            /**
             * ListView嵌套EditText真的有非常多的问题，下面部分的代码真的是调试很多遍才达到了理想的效果
             * 具体原因就现不阐述了
             */
            etInputTelephone.clearFocus();
            if (i == mCurrentInputPosition) {
                mCurrentEditText = etInputTelephone;
            }

            if (i == data.size() - 1) {
                if (mCurrentEditText != null) {
                    mCurrentEditText.requestFocus();
                    if (mCurrentEditText.getText().toString().length() == data.get(mCurrentInputPosition).length()) {
                        mCurrentEditText.setSelection(data.get(mCurrentInputPosition).length());
                    }
                }
            }

            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    mCurrentInputPosition = i;

                    String telephone = etInputTelephone.getText().toString().trim();

                    data.set(i, telephone);

                    if (!TextUtils.isEmpty(telephone)) {
                        if (i == data.size() - 1) {
                            // 最后一个输入框，不为空，新增一个输入框
                            data.add(i + 1, "");
                        }
                    }
                    notifyDataSetChanged();
                }
            };
            etInputTelephone.addTextChangedListener(textWatcher);
            etInputTelephone.setTag(textWatcher);
            return view;
        }
    }
}
