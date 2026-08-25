package com.RobinNotBad.BiliClient.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.BuildConfig;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.InstanceActivity;
import com.RobinNotBad.BiliClient.activity.settings.setup.SetupUIActivity;
import com.RobinNotBad.BiliClient.activity.video.RecommendActivity;
import com.RobinNotBad.BiliClient.activity.video.local.LocalListActivity;
import com.RobinNotBad.BiliClient.api.AppInfoApi;
import com.RobinNotBad.BiliClient.api.CookieRefreshApi;
import com.RobinNotBad.BiliClient.api.CookiesApi;
import com.RobinNotBad.BiliClient.util.BackupUtil;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

//启动页面
//一切的一切的开始

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends Activity {

    private TextView splashTextView;
    private int splashFrame;
    private Timer splashTimer;
    private String splashText = "欢迎使用\n哔哩终端";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(BiliTerminal.getFitDisplayContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_BiliClient);
        setContentView(R.layout.activity_splash);
        Log.e("debug", "进入应用");

        splashTextView = findViewById(R.id.splashText);
        splashText = SharedPreferencesUtil.getString("ui_splashtext", "欢迎使用\n哔哩终端");

        splashTimer = new Timer();
        splashTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> showSplashText(splashFrame));
                splashFrame++;
                if (splashFrame > splashText.length()) this.cancel();
            }
        }, 100, 100);

        CenterThreadPool.run(() -> {

            //FileUtil.clearCache(this);  //先清个缓存（为了防止占用过大）
            //不需要了，我把大部分图片的硬盘缓存都关闭了，只有表情包保留，这样既可以缩减缓存占用又能在一定程度上减少流量消耗

            if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.setup, false)) {//判断是否设置完成
                try {
                    // 未登录时请求bilibili.com
                    if (SharedPreferencesUtil.getLong("mid", 0) != 0) {
                        checkCookieRefresh();
                    }

                    CookiesApi.checkCookies();

                    String firstActivity = null;
                    String sortConf = SharedPreferencesUtil.getString(SharedPreferencesUtil.MENU_SORT, "");
                    if (!TextUtils.isEmpty(sortConf)) {
                        String[] splitName = sortConf.split(";");
                        for (String name : splitName) {
                            if (!MenuActivity.btnNames.containsKey(name)) {
                                for (Map.Entry<String, Pair<String, Class<? extends InstanceActivity>>> entry : MenuActivity.btnNames.entrySet()) {
                                    firstActivity = entry.getKey();
                                    break;
                                }
                            } else {
                                firstActivity = name;
                            }
                            break;
                        }
                    } else {
                        for (Map.Entry<String, Pair<String, Class<? extends InstanceActivity>>> entry : MenuActivity.btnNames.entrySet()) {
                            firstActivity = entry.getKey();
                            break;
                        }
                    }

                    Class<? extends InstanceActivity> activityClass = Objects.requireNonNull(MenuActivity.btnNames.get(firstActivity)).second;

                    Intent intent = new Intent();
                    intent.setClass(SplashActivity.this, (activityClass != null ? activityClass : RecommendActivity.class));
                    intent.putExtra("from", firstActivity);

                    interruptSplash();

                    splashTextView.postDelayed(() -> {
                        startActivity(intent);
                        CenterThreadPool.run(() -> AppInfoApi.check(SplashActivity.this));
                        finish();
                    }, 100);

                } catch (IOException e) {
                    runOnUiThread(() -> {
                        MsgUtil.err(e);
                        interruptSplash();
                        splashTextView.setText("网络错误");
                        if (SharedPreferencesUtil.getBoolean("setup", false)) {
                            splashTextView.postDelayed(() -> {
                                Intent intent = new Intent();
                                intent.setClass(SplashActivity.this, LocalListActivity.class);
                                startActivity(intent);
                                finish();
                            }, 300);
                        }
                    });
                } catch (JSONException e) {
                    runOnUiThread(() -> MsgUtil.err(e));
                    Intent intent = new Intent();
                    intent.setClass(SplashActivity.this, LocalListActivity.class);
                    startActivity(intent);
                    interruptSplash();
                    finish();
                }
            } else {
                // 未设置完成(未登录)。测试版尝试从 LoginData.txt 恢复登录信息，实现更新版本后免登录
                boolean loginRestored = false;
                if (BuildConfig.BETA) {
                    try {
                        String loginData = FileUtil.loadLoginData();
                        if (!TextUtils.isEmpty(loginData)) {
                            JSONObject loginJson = new JSONObject(loginData);
                            String cookies = loginJson.getString("cookies");
                            if (!TextUtils.isEmpty(cookies)) {
                                SharedPreferencesUtil.putLong(SharedPreferencesUtil.mid, Long.parseLong(NetWorkUtil.getInfoFromCookie("DedeUserID", cookies)));
                                SharedPreferencesUtil.putString(SharedPreferencesUtil.csrf, NetWorkUtil.getInfoFromCookie("bili_jct", cookies));
                                SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, cookies);
                                if (loginJson.has("refresh_token"))
                                    SharedPreferencesUtil.putString(SharedPreferencesUtil.refresh_token, loginJson.getString("refresh_token"));
                                SharedPreferencesUtil.putBoolean(SharedPreferencesUtil.setup, true);
                                NetWorkUtil.refreshHeaders();
                                loginRestored = true;
                                Log.e("LoginData", "已从 LoginData.txt 恢复登录");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("LoginData", "恢复登录失败: " + e.getMessage());
                    }
                }

                // 首次打开：若检测到 /Documents/BiliClient 内有备份文件，询问是否加载（仅出现一次）
                final boolean loginRestoredFinal = loginRestored;
                if (!SharedPreferencesUtil.getBoolean("backup_prompt_shown", false) && FileUtil.hasBackupFiles()) {
                    SharedPreferencesUtil.putBoolean("backup_prompt_shown", true); // 标记已询问，确保仅一次
                    runOnUiThread(() -> showRestorePromptDialog(loginRestoredFinal));
                    return;
                }

                Intent intent = new Intent();
                if (loginRestored) {
                    intent.setClass(SplashActivity.this, RecommendActivity.class);
                } else {
                    intent.setClass(SplashActivity.this, SetupUIActivity.class);   //没登录，去初次设置
                }
                startActivity(intent);
                interruptSplash();
                finish();
            }

        });
    }

    /**
     * 首次打开时，询问用户是否从 /Documents/BiliClient 加载备份文件。
     * 仅在检测到备份文件时调用一次。
     */
    private void showRestorePromptDialog(boolean loginRestored) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("发现备份文件");
        builder.setMessage("检测到 /Documents/BiliClient 目录下存在设置/教程备份文件，是否加载？");
        builder.setPositiveButton("加载", (dialog, which) -> {
            CenterThreadPool.run(() -> BackupUtil.restoreOnly());
            MsgUtil.showMsgLong("正在加载备份...");
            continueAfterPrompt(loginRestored);
        });
        builder.setNegativeButton("跳过", (dialog, which) -> continueAfterPrompt(loginRestored));
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /** 备份询问结束后的后续跳转 */
    private void continueAfterPrompt(boolean loginRestored) {
        Intent intent = new Intent();
        if (loginRestored) {
            intent.setClass(SplashActivity.this, RecommendActivity.class);
        } else {
            intent.setClass(SplashActivity.this, SetupUIActivity.class);
        }
        startActivity(intent);
        interruptSplash();
        finish();
    }

    private void checkCookieRefresh() throws IOException {
        try {
            JSONObject cookieInfo = CookieRefreshApi.cookieInfo();
            if (cookieInfo.optBoolean("refresh")) {
                Log.e("Cookies", "需要刷新");
                if (!Objects.equals(SharedPreferencesUtil.getString(SharedPreferencesUtil.refresh_token, ""), "")) {
                    String correspondPath = CookieRefreshApi.getCorrespondPath(cookieInfo.getLong("timestamp"));
                    Log.e("CorrespondPath", correspondPath);
                    String refreshCsrf = CookieRefreshApi.getRefreshCsrf(correspondPath);
                    Log.e("RefreshCsrf", refreshCsrf);
                    if (CookieRefreshApi.refreshCookie(refreshCsrf)) {
                        MsgUtil.showMsg("Cookies已刷新");
                    } else {
                        MsgUtil.showMsgLong("登录信息过期，请重新登录！");
                        resetLogin();
                    }
                }
            }
        } catch (JSONException e) {
            MsgUtil.showMsgLong("登录信息过期，请重新登录！");
            resetLogin();
        }
    }

    private void resetLogin() {
        SharedPreferencesUtil.putLong(SharedPreferencesUtil.mid, 0L);
        SharedPreferencesUtil.putString(SharedPreferencesUtil.csrf, "");
        SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, "");
        SharedPreferencesUtil.putString(SharedPreferencesUtil.refresh_token, "");
        NetWorkUtil.refreshHeaders();
    }

    @SuppressLint("SetTextI18n")
    private void showSplashText(int i) {
        if (i > splashText.length()) splashTextView.setText(splashText);
        else splashTextView.setText(splashText.substring(0, i) + "_");
    }

    private void interruptSplash() {
        if (splashTimer != null) splashTimer.cancel();
        splashTimer = null;
        runOnUiThread(() -> splashTextView.setText(splashText));
    }
}