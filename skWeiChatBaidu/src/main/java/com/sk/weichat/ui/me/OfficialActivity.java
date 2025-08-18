package com.sk.weichat.ui.me;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.User;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.message.ChatActivity;
import com.sk.weichat.ui.other.BasicInfoActivity;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.SkinTextView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;

public class OfficialActivity extends BaseActivity {

    private RecyclerView rvPublicNumber;
    private PublicAdapter publicAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_official);
        SkinTextView tv_title_center = findViewById(R.id.tv_title_center);
        tv_title_center.setTextColor(getResources().getColor(R.color.black));
        tv_title_center.setText("公众号");
        rvPublicNumber = findViewById(R.id.rvPublicNumber);
        rvPublicNumber.setHasFixedSize(true);
        rvPublicNumber.setNestedScrollingEnabled(false);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        rvPublicNumber.setLayoutManager(lm);
        publicAdapter = new PublicAdapter();
        rvPublicNumber.setAdapter(publicAdapter);
        requestServiceNumber();

    }

    private void requestServiceNumber() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        DialogHelper.showDefaulteMessageProgressDialogAddCancel(OfficialActivity.this, null);

        HttpUtils.get().url(coreManager.getConfig().PUBLIC_SEARCH)
                .params(params)
                .build()
                .execute(new ListCallback<User>(User.class) {
                    @Override
                    public void onResponse(ArrayResult<User> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(OfficialActivity.this, result)) {
                            List<User> list = result.getData();
                            publicAdapter.setData(list);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(OfficialActivity.this);
                    }
                });
    }

    private static class PublicViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHead = itemView.findViewById(R.id.notice_iv);
        TextView tvName = itemView.findViewById(R.id.notice_tv);

        PublicViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class PublicAdapter extends RecyclerView.Adapter<PublicViewHolder> {
        private List<User> data = Collections.emptyList();

        public void setData(List<User> data) {
            this.data = new ArrayList<>(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PublicViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new PublicViewHolder(getLayoutInflater().inflate(R.layout.item_square_public_number, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PublicViewHolder vh, int i) {
            User item = data.get(i);
            Friend friend = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), item.getUserId());
            if (friend != null) {
                vh.tvName.setText(TextUtils.isEmpty(friend.getRemarkName()) ? item.getNickName() : friend.getRemarkName());
            } else {
                vh.tvName.setText(item.getNickName());
            }
            AvatarHelper.getInstance().displayAvatar(item.getNickName(), item.getUserId(), vh.ivHead, true);

            vh.itemView.setOnClickListener(v -> {
                Friend friend2 = FriendDao.getInstance().getFriend(coreManager.getSelf().getUserId(), item.getUserId());
                if (friend2 != null && (friend2.getStatus() == Friend.STATUS_FRIEND || friend2.getStatus() == Friend.STATUS_SYSTEM)) {
                    ChatActivity.start(OfficialActivity.this, friend2);
                } else {
                    BasicInfoActivity.start(OfficialActivity.this, item.getUserId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
