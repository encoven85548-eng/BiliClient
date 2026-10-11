package com.RobinNotBad.BiliClient.activity.video;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;
import com.RobinNotBad.BiliClient.adapter.QualityChooseAdapter;
import com.RobinNotBad.BiliClient.api.PlayerApi;
import com.RobinNotBad.BiliClient.model.PlayerData;
import com.RobinNotBad.BiliClient.model.VideoInfo;
import com.RobinNotBad.BiliClient.ui.widget.recycler.CustomLinearManager;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.TerminalContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 缓存（下载）前的清晰度选择页。
 * <p>
 * 以前的实现要等"视频信息 + 清晰度列表"都取回来之后才创建 adapter，
 * 期间页面是一片空白，低性能设备上看起来就像点下去直接假死。
 * 现在改成：
 * 1. 先用 activity_loading 占位，列表布局由 AsyncLayoutInflater 在后台解析，页面立刻可见；
 * 2. 如果详情页已经把带清晰度列表的 PlayerData 传进来了，直接就用它显示，连网络都不用等；
 *    没有的话才挂一个"正在获取清晰度…"的占位项，取回后再替换；
 * 3. 获取失败时给出提示并退出页面（已经在用缓存列表的话就继续用，不会把人踢出去）。
 * 网络请求依旧全部在 CenterThreadPool 里异步执行，不会阻塞主线程。
 */
public class QualityChooserActivity extends BaseActivity {

    private int[] qns;
    private boolean isAudioOnlyOption = false;
    /** 清晰度列表是否已取回。未取回时列表里是占位项，点击无效 */
    private boolean loaded = false;

    private VideoInfo videoInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final long aid = getIntent().getLongExtra("aid", 0);
        final String bvid = getIntent().getStringExtra("bvid");
        final int page = getIntent().getIntExtra("page", 0);
        // 视频详情页点"缓存"时会把已经取好的 PlayerData 一起传进来（和分页选集页一样），
        // 里面带着详情页加载时就已经请求到的清晰度列表，可以先把列表显示出来，
        // 不用干等一次 playurl 网络请求 —— 慢网络上那正好就是"点完要等两秒才弹出列表"。
        final PlayerData cachedData = getIntent().getParcelableExtra("data");

        asyncInflate(R.layout.activity_simple_list, (layoutView, id) -> {
            RecyclerView recyclerView = findViewById(R.id.recyclerView);
            findViewById(R.id.top).setOnClickListener(view -> {
                setResult(RESULT_CANCELED);
                finish();
            });

            ((TextView) findViewById(R.id.pageName)).setText("请选择清晰度");

            final QualityChooseAdapter adapter = new QualityChooseAdapter(this);
            recyclerView.setLayoutManager(new CustomLinearManager(this));
            recyclerView.setAdapter(adapter);

            if (cachedData != null && cachedData.qnStrList != null && cachedData.qnValueList != null
                    && cachedData.qnStrList.length > 0) {
                // 有缓存列表：立刻就能点，不用等网络
                List<String> cachedQualityList = new ArrayList<>(Arrays.asList(cachedData.qnStrList));
                cachedQualityList.add("仅音频");
                qns = cachedData.qnValueList;
                isAudioOnlyOption = true;
                adapter.setNameList(cachedQualityList);
                loaded = true;
            } else {
                // 没有缓存列表：先显示占位项，页面不必等清晰度列表
                List<String> loadingList = new ArrayList<>();
                loadingList.add("正在获取清晰度…");
                adapter.setNameList(loadingList);
            }

            adapter.setOnItemClickListener(position -> {
                if (!loaded || qns == null || videoInfo == null) return; // 占位项或还没加载完，忽略点击

                if (isAudioOnlyOption && position == qns.length) {
                    // 获取DASH格式数据
                    CenterThreadPool.run(() -> {
                        try {
                            PlayerData playerData = videoInfo.toPlayerData(page);
                            playerData.qn = qns[0]; // 使用最高清晰度获取音频
                            PlayerApi.getVideoDash(playerData);

                            if (playerData.audioUrl == null || playerData.audioUrl.isEmpty()) {
                                runOnUiThread(() -> MsgUtil.showMsg("该视频没有可用的音频流"));
                                return;
                            }

                            PlayerApi.startDownloadingAudioOnly(videoInfo, page, qns[0], playerData.audioUrl);
                            runOnUiThread(this::finish);
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                MsgUtil.showMsg("获取音频信息失败：" + e.getMessage());
                                e.printStackTrace();
                            });
                        }
                    });
                } else {
                    if (position < 0 || position >= qns.length) return;
                    // 选择了普通清晰度
                    int qn = qns[position];
                    PlayerApi.startDownloading(videoInfo, page, qn);
                    finish();
                }
            });

            TerminalContext.getInstance().getVideoInfoByAidOrBvId(aid, bvid).observe(this,
                    result -> result.onSuccess(info -> {
                        videoInfo = info;
                        CenterThreadPool.run(() -> {
                            // 获取清晰度列表
                            try {
                                PlayerData playerData = videoInfo.toPlayerData(page);
                                PlayerApi.getVideo(playerData, true);
                                if (playerData.qnValueList == null || playerData.qnStrList == null)
                                    throw new IllegalStateException("empty quality list");

                                final int[] qnValues = playerData.qnValueList;
                                List<String> qualityList = new ArrayList<>(Arrays.asList(playerData.qnStrList));
                                // 在清晰度列表末尾添加"仅音频"选项
                                qualityList.add("仅音频");

                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    qns = qnValues;
                                    isAudioOnlyOption = true;
                                    adapter.setNameList(qualityList);
                                    loaded = true;
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    if (loaded) {
                                        // 已经在用详情页带过来的清晰度列表了，刷新失败就继续用旧的，
                                        // 不能因为刷新失败把用户从这个还能用的页面踢出去
                                        Logu.d("刷新清晰度列表失败，继续使用缓存列表");
                                    } else {
                                        MsgUtil.showMsg("清晰度列表获取失败！");
                                        finish();
                                    }
                                });
                            }
                        });
                    }).onFailure(error -> {
                        if (isFinishing() || isDestroyed()) return;
                        MsgUtil.err("获取视频信息失败：", error);
                        finish();
                    }));

        });
    }

}
