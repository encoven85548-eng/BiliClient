package com.RobinNotBad.BiliClient.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.BaseActivity;

import java.util.ArrayList;

/**
 * 通用「提示 + 选择」页，用来替代系统 AlertDialog。
 * <p>
 * 为什么不用 AlertDialog：
 * 1. 圆表上系统对话框的标题/左下/右下内容会被圆形屏幕裁掉，按钮经常点不到；
 * 2. 部分手表系统默认 DPI 很大，系统对话框算出来的尺寸超过屏幕，内容显示不全；
 * 3. 少数设备系统把 AlertDialog 相关组件精简掉了，直接崩。
 * <p>
 * 所以这里用应用自己的设计语言（顶栏 + 卡片 + 设置页同款按钮卡片）做成一个普通页面，
 * 内容放在 RotaryScrollView 里可以滚动，圆表上也能看到全部内容。
 * <p>
 * 用法：
 * <pre>
 * Intent intent = new Intent(context, ChooseDialogActivity.class)
 *         .putExtra(ChooseDialogActivity.EXTRA_TITLE, "加载完成")
 *         .putExtra(ChooseDialogActivity.EXTRA_CONTENT, "……")
 *         .putStringArrayListExtra(ChooseDialogActivity.EXTRA_ITEMS, items);
 * launcher.launch(intent);
 * </pre>
 * 结果：选中后 RESULT_OK，带 {@link #RESULT_POSITION}（int）与 {@link #RESULT_ITEM}（String）；
 * 点顶栏或返回键退出为 RESULT_CANCELED。
 */
public class ChooseDialogActivity extends BaseActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_ITEMS = "items";

    public static final String RESULT_ITEM = "item";
    public static final String RESULT_POSITION = "position";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_dialog);

        Intent intent = getIntent();

        TextView pageName = findViewById(R.id.pageName);
        String title = intent.getStringExtra(EXTRA_TITLE);
        if (!TextUtils.isEmpty(title)) pageName.setText(title);

        String content = intent.getStringExtra(EXTRA_CONTENT);
        if (TextUtils.isEmpty(content)) {
            findViewById(R.id.contentCard).setVisibility(View.GONE);
        } else {
            ((TextView) findViewById(R.id.content)).setText(content);
        }

        final ArrayList<String> items = intent.getStringArrayListExtra(EXTRA_ITEMS);
        if (items == null || items.isEmpty()) {
            finish();
            return;
        }

        LinearLayout chooseLayout = findViewById(R.id.chooseLayout);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < items.size(); i++) {
            final int position = i;
            // 复用设置页的按钮卡片，保持与其它页面一致的外观
            View itemView = inflater.inflate(R.layout.cell_setting_button, chooseLayout, false);
            ((TextView) itemView.findViewById(R.id.setting_button_text)).setText(items.get(position));
            itemView.setOnClickListener(view -> {
                setResult(RESULT_OK, new Intent()
                        .putExtra(RESULT_POSITION, position)
                        .putExtra(RESULT_ITEM, items.get(position)));
                finish();
            });
            chooseLayout.addView(itemView);
        }
    }
}
