package com.sk.weichat.ui.map;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.sk.weichat.AppConstant;
import com.sk.weichat.BuildConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.NearPositionAdapter;
import com.sk.weichat.adapter.NearSearchPositionAdapter;
import com.sk.weichat.map.MapHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.tool.ButtonColorChange;
import com.sk.weichat.util.FileUtil;
import com.sk.weichat.util.PermissionUtil;
import com.sk.weichat.util.ScreenUtil;
import com.sk.weichat.util.SoftKeyBoardListener;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.ClearEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Administrator on 2017/7/20.
 */
public class MapPickerActivity extends BaseActivity {
    private static final String TAG = "MapPickerActivity";
    private boolean isChat;// 是否为聊天界面跳转，文字描述修改为发送
    private TextView tvTitleRight;
    private ImageView ivReturn;
    private ClearEditText mSearchEdit;
    private TextView mTvNotShowLocation;
    private RecyclerView mRecyclerView;
    private NearPositionAdapter mNearPositionAdapter;
    private RecyclerView mSearchRecyclerView;
    private NearSearchPositionAdapter mNearSearchPositionAdapter;

    private MapHelper mapHelper;
    private MapHelper.Picker picker;
    private MapHelper.LatLng beginLatLng;// 首次进入时的地理位置
    private MapHelper.LatLng currentLatLng;

    private List<MapHelper.Place> data = new ArrayList<>();
    private List<MapHelper.Place> searchData = new ArrayList<>();
    private boolean showTitle = true;
    private MapHelper.Place mPlace;
    private String mCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtil.requestLocationPermissions(this, 0x01);
        setContentView(R.layout.activity_map_picker);
        isChat = getIntent().getBooleanExtra(AppConstant.EXTRA_FORM_CAHT_ACTIVITY, false);
        initActionBar();
        initView();
        initMap();
        initEvent();
        if (BuildConfig.DEBUG) {
            com.sk.weichat.util.LogUtils.log("after create");
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.location));
        tvTitleRight = findViewById(R.id.tv_title_right);
        tvTitleRight.setText(isChat ? getString(R.string.send) : getResources().getString(R.string.sure));
        tvTitleRight.setBackground(mContext.getResources().getDrawable(R.drawable.bg_btn_grey_circle));
        ButtonColorChange.colorChange(mContext, tvTitleRight);
        tvTitleRight.setTextColor(getResources().getColor(R.color.white));
        tvTitleRight.setOnClickListener(v -> {
            if (picker != null) {
                View mapView = picker.getMapView();
                int dw = mapView.getWidth();
                int dh = mapView.getHeight();
                // 截取宽度一半，
                int width = dw / 2;
                // 图片宽高比要和视图一样，
                int height = (int) (width * 1f / 672 * 221);
                // 以防万一，等比例缩小至全屏，
                float scale = Math.max(1, Math.min(width * 1.0f / dw, height * 1.0f / dh));
                final int rw = (int) (width / scale);
                final int rh = (int) (height / scale);

                int left = (dw - rw) / 2;
                int right = (dw + rw) / 2;
                int top = (dh - rh) / 2;
                int bottom = (dh + rh) / 2;
                Rect rect = new Rect(left, top, right, bottom);
                picker.snapshot(rect, bitmap -> {
                    // 部分截图保存本地，
                    String snapshot = FileUtil.saveBitmap(bitmap);
                    if (mPlace == null) {
                        if (data.size() > 0) {// 默认选中第一个
                            mPlace = data.get(0);
                        }
                    }
                    // 位置获取失败也返回空，外面有判断和提示，不能直接return没反应，
                    String address;
                    if (mPlace != null) {
                        address = mPlace.getName();
                    } else {
                        address = "";
                    }
                    if (TextUtils.isEmpty(address)) {
                        address = MyApplication.getInstance().getBdLocationHelper().getAddress();
                    }
                    Intent intent = new Intent();
                    intent.putExtra(AppConstant.EXTRA_LATITUDE, currentLatLng.getLatitude());
                    intent.putExtra(AppConstant.EXTRA_LONGITUDE, currentLatLng.getLongitude());
                    intent.putExtra(AppConstant.EXTRA_ADDRESS, address);
                    intent.putExtra(AppConstant.EXTRA_SNAPSHOT, snapshot);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }
        });
        // 一切准备就绪在显示发送按钮
        tvTitleRight.setVisibility(View.GONE);
    }

    public void initView() {
        ivReturn = findViewById(R.id.iv_location);
        // 一切准备就绪在显示回到当前位置按钮
        ivReturn.setVisibility(View.GONE);
        mSearchEdit = findViewById(R.id.ce_map_position);
        mSearchEdit.clearFocus();
        mTvNotShowLocation = findViewById(R.id.tvNotShowLocation);
        mTvNotShowLocation.setVisibility(isChat ? View.GONE : View.VISIBLE);

        mRecyclerView = findViewById(R.id.rv_map_position);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MapPickerActivity.this));
        mNearPositionAdapter = new NearPositionAdapter(this);
        mNearPositionAdapter.setOnItemClickedListener(place -> {
            mPlace = place;
            currentLatLng = place.getLatLng();
            picker.moveMap(currentLatLng);
        });
        mRecyclerView.setAdapter(mNearPositionAdapter);

        mSearchRecyclerView = findViewById(R.id.rv_map_position_search);
        mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(MapPickerActivity.this));
        mNearSearchPositionAdapter = new NearSearchPositionAdapter(this);
        mNearSearchPositionAdapter.setOnItemClickedListener(place -> {
            mPlace = place;
            currentLatLng = place.getLatLng();
            picker.moveMap(currentLatLng);
        });
        mSearchRecyclerView.setAdapter(mNearSearchPositionAdapter);
    }

    private void initMap() {
        mapHelper = MapHelper.getInstance();
        picker = mapHelper.getPicker(this);
        getLifecycle().addObserver(picker);
        picker.attack(findViewById(R.id.map_view_container), () -> {
            // 初始化底部周边相关动画，
            // 中心打上图标，
            picker.addCenterMarker(R.drawable.ic_position, "pos");
            mapHelper.requestLatLng(latLng -> {
                // 记录开始时定位的位置，用来点击按钮跳回来，
                beginLatLng = latLng;
                picker.moveMap(beginLatLng);
                // 加载周边位置信息，
                // 记录当前位置也在这个方法里，
                loadMapDatas(beginLatLng);
                getCity();
            }, t -> {
                ToastUtil.showToast(MapPickerActivity.this, getString(R.string.tip_auto_location_failed) + t.getMessage());
                // 总有个默认的经纬度，拿出来，
                beginLatLng = picker.currentLatLng();
                picker.moveMap(beginLatLng);
                loadMapDatas(beginLatLng);
                getCity();
            });
        });
        picker.setOnMapStatusChangeListener(new MapHelper.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapHelper.MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChange(MapHelper.MapStatus mapStatus) {
            }

            @Override
            public void onMapStatusChangeFinish(MapHelper.MapStatus mapStatus) {
                loadMapDatas(mapStatus.target);
            }
        });
    }

    /**
     * @param latLng
     */
    private void loadMapDatas(MapHelper.LatLng latLng) {
        mPlace = null;
        currentLatLng = latLng;
        tvTitleRight.setVisibility(View.VISIBLE);
        ivReturn.setVisibility(View.VISIBLE);

        mapHelper.requestPlaceList(latLng, places -> {
            data.clear();
            data.addAll(places);
            mNearPositionAdapter.setData(data);
        }, t -> ToastUtil.showToast(MapPickerActivity.this, getString(R.string.tip_places_around_failed) + t.getMessage()));
    }

    private void initEvent() {
        ivReturn.setOnClickListener(v -> {
            picker.moveMap(beginLatLng);
            loadMapDatas(beginLatLng);
        });

        mSearchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = mSearchEdit.getText().toString().trim();
                if (TextUtils.isEmpty(keyword)) {
                    searchData.clear();
                    mNearSearchPositionAdapter.setData(searchData);
                    return;
                }
                loadSearch(keyword);
            }
        });

        mTvNotShowLocation.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra(AppConstant.EXTRA_ADDRESS, String.valueOf(-1));
            setResult(RESULT_OK, intent);
            finish();
        });

        SoftKeyBoardListener.setListener(this, new SoftKeyBoardListener.OnSoftKeyBoardChangeListener() {

            @Override
            public void keyBoardShow(int height) {
                startTranslateAnim(false);
                findViewById(R.id.tv_keyboard).setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
                mSearchRecyclerView.setVisibility(View.VISIBLE);
                searchData.clear();
                mNearSearchPositionAdapter.setData(searchData);
            }

            @Override
            public void keyBoardHide(int height) {
                startTranslateAnim(true);
                findViewById(R.id.tv_keyboard).setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
                mSearchRecyclerView.setVisibility(View.GONE);
                // 搜索状态解除，回到最开始位置
                // todo 或者也可以load 当前已选中的经纬度地址
                mSearchEdit.setText("");
                picker.moveMap(beginLatLng);
                loadMapDatas(beginLatLng);
            }
        });
    }

    /**
     * 百度地图根据关键字搜索的方法
     *
     * @param keyword todo 待调试
     */
    private void loadSearch(String keyword) {
        SuggestionSearch suggestionSearch = SuggestionSearch.newInstance();
        suggestionSearch.setOnGetSuggestionResultListener(suggestionResult -> {
            searchData.clear();
            if (suggestionResult.getAllSuggestions() != null) {
                for (int i = 0; i < suggestionResult.getAllSuggestions().size(); i++) {
                    SuggestionResult.SuggestionInfo suggestionInfo = suggestionResult.getAllSuggestions().get(i);
                    MapHelper.LatLng latLng;
                    if (suggestionInfo.getPt() != null) {
                        latLng = new MapHelper.LatLng(suggestionInfo.getPt().latitude, suggestionInfo.getPt().longitude);
                    } else {
                        latLng = new MapHelper.LatLng(0, 0);
                    }
                    // City：城市 District：区/县 Key：地名
                    MapHelper.Place place = new MapHelper.Place(suggestionInfo.getKey(),
                            suggestionInfo.getCity() + suggestionInfo.getDistrict() + suggestionInfo.getKey(),
                            latLng);
                    searchData.add(place);
                }
            }
            mNearSearchPositionAdapter.setData(searchData);
        });
        suggestionSearch.requestSuggestion(new SuggestionSearchOption().keyword(keyword)
                .city(TextUtils.isEmpty(mCity) ? "深圳市" : mCity)
                .location(new LatLng(beginLatLng.getLatitude(), beginLatLng.getLongitude())));
    }

    /**
     * 通过初始经纬度获取城市，用于搜索
     */
    private void getCity() {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        new Thread(() -> {
            try {
                List<Address> list = geocoder.getFromLocation(beginLatLng.getLatitude(),
                        beginLatLng.getLongitude(), 1);
                if (list != null && list.size() > 0) {
                    mCity = list.get(0).getLocality();
                    Log.e(TAG, "初始经纬度所在城市:" + mCity);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void startTranslateAnim(boolean show) {
        if (showTitle == show) {
            return;
        }
        showTitle = show;
        float fromY = -(ScreenUtil.getScreenHeight(mContext) / 3);
        float toY = 0;

        if (!show) {
            fromY = 0;
            toY = -(ScreenUtil.getScreenHeight(mContext) / 3);
        }

        ObjectAnimator animator = ObjectAnimator.ofFloat(findViewById(R.id.ll), "translationY", fromY, toY);
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(findViewById(R.id.iv_location), "translationY", fromY, toY);
        animator.setDuration(300);
        animator2.setDuration(300);
        animator.start();
        animator2.start();
    }

    public void cancelKeyBoard(View view) {
        // 点击空白区域隐藏软键盘
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(findViewById(R.id.tv_keyboard).getWindowToken(), 0); //强制隐藏键盘
        }
    }
}
