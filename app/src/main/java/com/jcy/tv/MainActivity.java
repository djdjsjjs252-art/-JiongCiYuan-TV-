package com.jcy.tv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Native Android TV entry point. This deliberately contains no WebView.
 * Catalog and playback are kept behind a separate data layer until the
 * original app's authenticated API contract can be implemented safely.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(12, 15, 23);
    private static final int PANEL = Color.rgb(23, 29, 42);
    private static final int ACCENT = Color.rgb(126, 92, 255);
    private LinearLayout root;
    private LinearLayout content;
    private TextView pageTitle;
    private String currentPage = "首页";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        buildShell();
        showPage("首页");
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(30), dp(22), dp(30), dp(22));

        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.TOP);
        rail.setPadding(0, dp(4), dp(24), 0);
        TextView brand = label("囧次元", 27, Color.WHITE, true);
        rail.addView(brand, params(-1, dp(58)));

        String[] pages = {"首页", "番剧", "电影", "搜索", "设置"};
        for (String page : pages) {
            Button item = navButton(page);
            item.setOnClickListener(v -> showPage(page));
            rail.addView(item, params(dp(150), dp(54)));
        }
        root.addView(rail, params(dp(180), -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(4), dp(8), dp(12));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1f));
        setContentView(root);
    }

    private void showPage(String page) {
        currentPage = page;
        content.removeAllViews();
        pageTitle = label(page, 30, Color.WHITE, true);
        content.addView(pageTitle, params(-1, dp(58)));

        if ("搜索".equals(page)) {
            showSearch();
            return;
        }
        if ("设置".equals(page)) {
            addMessage("电视端基础设置", "当前版本正在搭建原生电视界面与数据层。");
            addMessage("内容接口", "原版 APK 使用 Flutter 编译代码，接口请求包含应用侧处理；尚未完成合法接口适配前，不会伪造影片列表或播放地址。");
            return;
        }
        if ("番剧".equals(page) || "电影".equals(page)) {
            addMessage(page + "内容", "内容服务尚未接通。接入原版可用的数据接口后，这里会显示真实分类与影片。");
            return;
        }
        addMessage("为电视遥控器重新设计", "原生 Android 页面 · 方向键移动 · OK 确认 · 返回上一级");
        addMessage("内容服务适配中", "已确认原版应用采用 Flutter，包含视频列表、搜索、详情、播放等独立接口路径。当前先搭建不依赖网页的电视端界面。");
    }

    private void showSearch() {
        TextView hint = label("搜索番剧或电影", 18, Color.LTGRAY, false);
        content.addView(hint, params(-1, dp(42)));
        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setTextColor(Color.WHITE);
        query.setHintTextColor(Color.GRAY);
        query.setHint("输入片名");
        query.setTextSize(20);
        query.setPadding(dp(16), 0, dp(16), 0);
        query.setBackgroundColor(PANEL);
        content.addView(query, params(-1, dp(58)));
        Button go = navButton("搜索");
        go.setOnClickListener(v -> {
            String text = query.getText().toString().trim();
            content.removeViews(2, content.getChildCount() - 2);
            addMessage("搜索：" + (text.isEmpty() ? "未输入关键词" : text),
                    "搜索服务尚未接通，接入原版可用接口后将显示真实结果。");
        });
        content.addView(go, params(dp(180), dp(54)));
        query.requestFocus();
    }

    private void addMessage(String title, String body) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(16), dp(22), dp(16));
        panel.setBackgroundColor(PANEL);
        TextView heading = label(title, 20, Color.WHITE, true);
        TextView description = label(body, 15, Color.LTGRAY, false);
        description.setPadding(0, dp(8), 0, 0);
        panel.addView(heading);
        panel.addView(description);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.bottomMargin = dp(14);
        content.addView(panel, p);
    }

    private Button navButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        b.setOnFocusChangeListener((v, focused) -> {
            v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    focused ? ACCENT : PANEL));
            v.setScaleX(focused ? 1.04f : 1f);
            v.setScaleY(focused ? 1.04f : 1f);
        });
        return b;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout.LayoutParams params(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onBackPressed() {
        if (!"首页".equals(currentPage)) showPage("首页");
        else super.onBackPressed();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
            onBackPressed();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
