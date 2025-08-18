package com.sk.weichat.ui.tool;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.sk.weichat.R;
import com.sk.weichat.fragment.WebViewFragment;
import com.sk.weichat.ui.base.BaseActivity;

public class WebGameActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_game);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(String.valueOf(0));
        if (fragment == null) {
            fragment = new WebViewFragment();
        }
        transaction.replace(R.id.webGameFragment, fragment);
        transaction.commit();
    }
}