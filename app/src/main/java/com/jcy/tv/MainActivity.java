package com.jcy.tv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

/**
 * Native Android TV client. Playback accepts a user-provided authorized media URL.
 * The original service's private/authenticated catalog API is not impersonated.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(12, 15, 23);
    private static final int PANEL = Color.rgb(23, 29, 42);
    private static final int ACCENT = Color.rgb(126, 92, 255);
    private LinearLayout root;
    private LinearLayout content;
    private EditText mediaUrl;
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView playbackStatus;
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
        rail.addView(label("囧次元 TV", 25, Color.WHITE, true), params(-1, dp(58)));
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
        content.addView(label(page, 30, Color.WHITE, true), params(-1, dp(58)));
        if ("首页".equals(page)) {
            addMessage("电视端播放器已接入", "支持 HLS（m3u8）、DASH（mpd）及常见网络视频格式。使用遥控器选择输入框，输入你有权访问的视频直链后播放。");
            showUrlPlayer();
        } else if ("搜索".equals(page)) {
            addMessage("内容搜索", "原版囧次元搜索服务需要有效的官方接口授权。目前尚未取得可独立调用的接口凭据，因此不会显示伪造的搜索结果。");
        } else if ("番剧".equals(page) || "电影".equals(page)) {
            addMessage(page + "内容库", "内容目录接口尚未完成合法接入。播放器已可用于播放你提供且有权访问的媒体直链。");
        } else {
            addMessage("播放设置", "使用 HTTPS 视频地址可直接播放。播放控制支持遥控器方向键、确认键和返回键。");
            addMessage("版本信息", "囧次元 TV 原生客户端 1.1.0 · Android Media3 播放引擎");
        }
    }

    private void showUrlPlayer() {
        content.addView(label("视频地址（m3u8 / mpd / mp4）", 17, Color.LTGRAY, false),
                params(-1, dp(38)));
        mediaUrl = new EditText(this);
        mediaUrl.setSingleLine(true);
        mediaUrl.setTextColor(Color.WHITE);
        mediaUrl.setHintTextColor(Color.GRAY);
        mediaUrl.setHint("https://example.com/video.m3u8");
        mediaUrl.setTextSize(16);
        mediaUrl.setSelectAllOnFocus(true);
        mediaUrl.setPadding(dp(14), 0, dp(14), 0);
        mediaUrl.setBackgroundColor(PANEL);
        content.addView(mediaUrl, params(-1, dp(54)));

        Button play = navButton("播放视频");
        play.setOnClickListener(v -> startPlayback());
        content.addView(play, params(dp(210), dp(52)));

        playbackStatus = label("尚未开始播放", 14, Color.LTGRAY, false);
        content.addView(playbackStatus, params(-1, dp(38)));

        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams videoParams = params(-1, dp(330));
        videoParams.topMargin = dp(8);
        content.addView(playerView, videoParams);
        playerView.setFocusable(true);
        playerView.setFocusableInTouchMode(true);
    }

    private void startPlayback() {
        String url = mediaUrl == null ? "" : mediaUrl.getText().toString().trim();
        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(this, "请输入有效的 http 或 https 视频地址", Toast.LENGTH_LONG).show();
            mediaUrl.requestFocus();
            return;
        }
        releasePlayer();
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (playbackStatus == null) return;
                if (state == Player.STATE_BUFFERING) playbackStatus.setText("正在缓冲…");
                else if (state == Player.STATE_READY) playbackStatus.setText("正在播放");
                else if (state == Player.STATE_ENDED) playbackStatus.setText("播放结束");
                else if (state == Player.STATE_IDLE) playbackStatus.setText("等待播放");
            }
            @Override public void onPlayerError(PlaybackException error) {
                if (playbackStatus != null) playbackStatus.setText("播放失败：" + error.getErrorCodeName());
            }
        });
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
        playbackStatus.setText("正在连接视频源…");
        playerView.requestFocus();
        View focused = getCurrentFocus();
        if (focused != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (playerView != null) playerView.setPlayer(null);
    }

    private void addMessage(String title, String body) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(14), dp(20), dp(14));
        panel.setBackgroundColor(PANEL);
        panel.addView(label(title, 19, Color.WHITE, true));
        TextView description = label(body, 14, Color.LTGRAY, false);
        description.setPadding(0, dp(7), 0, 0);
        panel.addView(description);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.bottomMargin = dp(12);
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

    @Override protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (player != null && player.isPlaying()) {
            player.pause();
            Toast.makeText(this, "已暂停", Toast.LENGTH_SHORT).show();
        } else if (!"首页".equals(currentPage)) showPage("首页");
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
