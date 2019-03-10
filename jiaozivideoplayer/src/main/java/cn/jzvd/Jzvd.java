package cn.jzvd;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Nathen on 16/7/30.
 */
public abstract class Jzvd extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

    public static final String TAG = "JZVD";
    public static final int THRESHOLD = 80;

    public static final int SCREEN_WINDOW_NORMAL = 0;
    public static final int SCREEN_WINDOW_LIST = 1;
    public static final int SCREEN_WINDOW_FULLSCREEN = 2;
    public static final int SCREEN_WINDOW_TINY = 3;

    public static final int CURRENT_STATE_IDLE = -1;
    public static final int CURRENT_STATE_NORMAL = 0;
    public static final int CURRENT_STATE_PREPARING = 1;
    public static final int CURRENT_STATE_PREPARING_CHANGING_URL = 2;
    public static final int CURRENT_STATE_PLAYING = 3;
    public static final int CURRENT_STATE_PAUSE = 5;
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    public static final int CURRENT_STATE_ERROR = 7;

    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER = 0;//default
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT = 1;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP = 2;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ORIGINAL = 3;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;//过一遍demo
    public static boolean SAVE_PROGRESS = true;
    public static boolean WIFI_TIP_DIALOG_SHOWED = false;
    public static int VIDEO_IMAGE_DISPLAY_TYPE = 0;
    public static long lastAutoFullscreenTime = 0;
    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {//是否新建个class，代码更规矩，并且变量的位置也很尴尬
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    resetAllVideos();
                    Log.d(TAG, "AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
//                    try {
//                        Jzvd player = JzvdMgr.getCurrentJzvd();
//                        if (player != null && player.currentState == Jzvd.CURRENT_STATE_PLAYING) {
//                            player.startButton.performClick();
//                        }
//                    } catch (IllegalStateException e) {
//                        e.printStackTrace();
//                    }
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };
    protected static JZUserAction JZ_USER_EVENT;
    protected Timer UPDATE_PROGRESS_TIMER;
    public int currentState = -1;
    public int currentScreen = -1;
    public long seekToInAdvance = 0;
    public ImageView startButton;
    public SeekBar progressBar;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup topContainer, bottomContainer;
    public int widthRatio = 0;
    public int heightRatio = 0;
    public JZDataSource jzDataSource;
    public int positionInList = -1;//很想干掉它
    public int videoRotation = 0;
    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected ProgressTimerTask mProgressTimerTask;
    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected long mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected long mSeekTimePosition;

    public JZMediaInterface mediaInterface;

    public static Jzvd CURRENT_JZVD;
    public static LinkedList CONTAINER_LIST = new LinkedList();

    public Jzvd(Context context) {
        super(context);
        init(context);
    }

    public Jzvd(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setMediaInterface(JZMediaInterface mediaInterface) {
        this.mediaInterface = mediaInterface;
    }

    public static void resetAllVideos() {
        Log.d(TAG, "resetAllVideos");
        if (Jzvd.CURRENT_JZVD != null) {
            Jzvd.CURRENT_JZVD.reset();
            CURRENT_JZVD = null;
        }
    }
//
//    public static void startFullscreen(Context context, Class _class, String url, String title) {
//        startFullscreen(context, _class, new JZDataSource(url, title));
//    }
//
//    public static void startFullscreen(Context context, Class _class, JZDataSource jzDataSource) {
//        hideStatusBar(context);
//        JZUtils.setRequestedOrientation(context, FULLSCREEN_ORIENTATION);
//        ViewGroup vp = (JZUtils.scanForActivity(context))//.getWindow().getDecorView();
//                .findViewById(Window.ID_ANDROID_CONTENT);
//        View old = vp.findViewById(R.id.jz_fullscreen_id);
//        if (old != null) {
//            vp.removeView(old);
//        }
//        try {
//            Constructor<Jzvd> constructor = _class.getConstructor(Context.class);
//            final Jzvd jzvd = constructor.newInstance(context);
//            jzvd.setId(R.id.jz_fullscreen_id);
//            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//            vp.addView(jzvd, lp);
////            final Animation ra = AnimationUtils.loadAnimation(context, R.anim.start_fullscreen);
////            jzVideoPlayer.setAnimation(ra);
//            jzvd.setUp(jzDataSource, JzvdStd.SCREEN_WINDOW_FULLSCREEN);
//            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
//            jzvd.startButton.performClick();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void clearDecorView() {
        ViewGroup vg = (ViewGroup) (JZUtils.scanForActivity(getContext())).getWindow().getDecorView();
        vg.removeView(CURRENT_JZVD);
    }

    public static boolean backPress() {
        Log.i(TAG, "backPress");
//        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
//            return false; 这些东西遇到了再改，最后过代码的时候删除残留
        if (CONTAINER_LIST.size() != 0) {
            CURRENT_JZVD.clearDecorView();
            //测试一下layoutparam的不同属性有什么区别
            ((ViewGroup) CONTAINER_LIST.getLast()).addView(CURRENT_JZVD);
            JZUtils.showStatusBar(CURRENT_JZVD.getContext());
//            CURRENT_JZVD.setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);//华为手机和有虚拟键的手机全屏时可隐藏虚拟键 issue:1326
//            showSystemUI((JZUtils.scanForActivity(CURRENT_JZVD.getContext())).getWindow().getDecorView());
            CONTAINER_LIST.pop();
            return true;
        }
        return false;
    }

    //    public static void quitFullscreenOrTinyWindow() {
//        //直接退出全屏和小窗
//        JzvdMgr.getFirstFloor().clearFloatScreen();
//        JZMediaPlayer.instance().releaseMediaPlayer();
//        JzvdMgr.completeAll();
//    }
//


    public static void clearSavedProgress(Context context, String url) {
        JZUtils.clearSavedProgress(context, url);
    }

    public static void setJzUserAction(JZUserAction jzUserEvent) {
        JZ_USER_EVENT = jzUserEvent;
    }

//    public static void goOnPlayOnResume() {
//        if (JzvdMgr.getCurrentJzvd() != null) {
//            Jzvd jzvd = JzvdMgr.getCurrentJzvd();
//            if (jzvd.currentState == Jzvd.CURRENT_STATE_PAUSE) {
//                if (ON_PLAY_PAUSE_TMP_STATE == CURRENT_STATE_PAUSE) {
//                    jzvd.onStatePause();
//                    JZMediaPlayer.pause();
//                } else {
//                    jzvd.onStatePlaying();
//                    JZMediaPlayer.start();
//                }
//                ON_PLAY_PAUSE_TMP_STATE = 0;
//            }
//        }
//    }
//
//    public static int ON_PLAY_PAUSE_TMP_STATE = 0;
//
//    public static void goOnPlayOnPause() {
//        if (JzvdMgr.getCurrentJzvd() != null) {
//            Jzvd jzvd = JzvdMgr.getCurrentJzvd();
//            if (jzvd.currentState == Jzvd.CURRENT_STATE_AUTO_COMPLETE ||
//                    jzvd.currentState == Jzvd.CURRENT_STATE_NORMAL ||
//                    jzvd.currentState == Jzvd.CURRENT_STATE_ERROR) {
////                JZVideoPlayer.resetAllVideos();
//            } else {
//                ON_PLAY_PAUSE_TMP_STATE = jzvd.currentState;
//                jzvd.onStatePause();
//                JZMediaPlayer.pause();
//            }
//        }
//    }

//    public static void onScrollAutoTiny(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//        int lastVisibleItem = firstVisibleItem + visibleItemCount;
//        int currentPlayPosition = JZMediaPlayer.instance().positionInList;
//        if (currentPlayPosition >= 0) {
//            if ((currentPlayPosition < firstVisibleItem || currentPlayPosition > (lastVisibleItem - 1))) {
//                if (JzvdMgr.getCurrentJzvd() != null &&
//                        JzvdMgr.getCurrentJzvd().currentScreen != Jzvd.SCREEN_WINDOW_TINY &&
//                        JzvdMgr.getCurrentJzvd().currentScreen != Jzvd.SCREEN_WINDOW_FULLSCREEN) {
//                    if (JzvdMgr.getCurrentJzvd().currentState == Jzvd.CURRENT_STATE_PAUSE) {
//                        Jzvd.resetAllVideos();
//                    } else {
//                        Log.e(TAG, "onScroll: out screen");
//                        JzvdMgr.getCurrentJzvd().startWindowTiny();
//                    }
//                }
//            } else {
//                if (JzvdMgr.getCurrentJzvd() != null &&
//                        JzvdMgr.getCurrentJzvd().currentScreen == Jzvd.SCREEN_WINDOW_TINY) {
//                    Log.e(TAG, "onScroll: into screen");
//                    Jzvd.backPress();
//                }
//            }
//        }
//    }
//
//    public static void onScrollReleaseAllVideos(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//        int lastVisibleItem = firstVisibleItem + visibleItemCount;
//        int currentPlayPosition = JZMediaPlayer.instance().positionInList;
//        Log.e(TAG, "onScrollReleaseAllVideos: " +
//                currentPlayPosition + " " + firstVisibleItem + " " + currentPlayPosition + " " + lastVisibleItem);
//        if (currentPlayPosition >= 0) {
//            if ((currentPlayPosition < firstVisibleItem || currentPlayPosition > (lastVisibleItem - 1))) {
//                if (JzvdMgr.getCurrentJzvd().currentScreen != Jzvd.SCREEN_WINDOW_FULLSCREEN) {
//                    Jzvd.resetAllVideos();//为什么最后一个视频横屏会调用这个，其他地方不会
//                }
//            }
//        }
//    }

//    public static void onChildViewAttachedToWindow(View view, int jzvdId) {
//        if (JzvdMgr.getCurrentJzvd() != null && JzvdMgr.getCurrentJzvd().currentScreen == Jzvd.SCREEN_WINDOW_TINY) {
//            Jzvd jzvd = view.findViewById(jzvdId);
//            if (jzvd != null && jzvd.jzDataSource.containsTheUrl(JZMediaPlayer.getCurrentUrl())) {
//                Jzvd.backPress();
//            }
//        }
//    }
//
//    public static void onChildViewDetachedFromWindow(View view) {
//        if (JzvdMgr.getCurrentJzvd() != null && JzvdMgr.getCurrentJzvd().currentScreen != Jzvd.SCREEN_WINDOW_TINY) {
//            Jzvd jzvd = JzvdMgr.getCurrentJzvd();
//            if (((ViewGroup) view).indexOfChild(jzvd) != -1) {
//                if (jzvd.currentState == Jzvd.CURRENT_STATE_PAUSE) {
//                    Jzvd.resetAllVideos();
//                } else {
//                    jzvd.startWindowTiny();
//                }
//            }
//        }
//    }

//    public static void setTextureViewRotation(int rotation) {
//        if (JzvdMgr.getCurrentJzvd() != null && JzvdMgr.getCurrentJzvd().textureView != null) {
//            JzvdMgr.getCurrentJzvd().textureView.setRotation(rotation);
//        }
//    }
//
//    public static void setVideoImageDisplayType(int type) {
//        Jzvd.VIDEO_IMAGE_DISPLAY_TYPE = type;
//        if (JzvdMgr.getCurrentJzvd() != null && JzvdMgr.getCurrentJzvd().textureView != null) {
//            JzvdMgr.getCurrentJzvd().textureView.requestLayout();
//        }
//    }

    public abstract int getLayoutId();

    public void init(Context context) {
        View.inflate(context, getLayoutId(), this);
        startButton = findViewById(R.id.start);
        fullscreenButton = findViewById(R.id.fullscreen);
        progressBar = findViewById(R.id.bottom_seek_progress);
        currentTimeTextView = findViewById(R.id.current);
        totalTimeTextView = findViewById(R.id.total);
        bottomContainer = findViewById(R.id.layout_bottom);
        textureViewContainer = findViewById(R.id.surface_container);
        topContainer = findViewById(R.id.layout_top);

        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);
        textureViewContainer.setOnTouchListener(this);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

//        try {
//            if (isCurrentPlay()) {
//                NORMAL_ORIENTATION = ((AppCompatActivity) context).getRequestedOrientation();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    public void setUp(String url, String title, int screen) {
        setUp(new JZDataSource(url, title), screen);
    }

    public void setUp(JZDataSource jzDataSource, int screen) {
        if (this.jzDataSource != null && jzDataSource.getCurrentUrl() != null &&
                this.jzDataSource.containsTheUrl(jzDataSource.getCurrentUrl())) {
            return;
        }
        this.jzDataSource = jzDataSource;
        this.currentScreen = screen;
        onStateNormal();

        mediaInterface = new JZMediaSystem(this);//这个位置可能需要调整
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            Log.i(TAG, "onClick start [" + this.hashCode() + "] ");
            if (jzDataSource == null || jzDataSource.urlsMap.isEmpty() || jzDataSource.getCurrentUrl() == null) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL) {
                if (!jzDataSource.getCurrentUrl().toString().startsWith("file") && !
                        jzDataSource.getCurrentUrl().toString().startsWith("/") &&
                        !JZUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {//这个可以放到std中
                    showWifiDialog();
                    return;
                }
                startVideo();
                onEvent(JZUserAction.ON_CLICK_START_ICON);//开始的事件应该在播放之后，此处特殊
            } else if (currentState == CURRENT_STATE_PLAYING) {
                onEvent(JZUserAction.ON_CLICK_PAUSE);
                Log.d(TAG, "pauseVideo [" + this.hashCode() + "] ");
                mediaInterface.pause();
                onStatePause();
            } else if (currentState == CURRENT_STATE_PAUSE) {
                onEvent(JZUserAction.ON_CLICK_RESUME);
                mediaInterface.start();
                onStatePlaying();
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onEvent(JZUserAction.ON_CLICK_START_AUTO_COMPLETE);
                startVideo();
            }
        } else if (i == R.id.fullscreen) {
            Log.i(TAG, "onClick fullscreen [" + this.hashCode() + "] ");
            if (currentState == CURRENT_STATE_AUTO_COMPLETE) return;
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                //quit fullscreen
                backPress();
            } else {
                Log.d(TAG, "toFullscreenActivity [" + this.hashCode() + "] ");
                onEvent(JZUserAction.ON_ENTER_FULLSCREEN);
                startWindowFullscreen();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "onTouch surfaceContainer actionDown [" + this.hashCode() + "] ");
                    mTouchingProgressBar = true;

                    mDownX = x;
                    mDownY = y;
                    mChangeVolume = false;
                    mChangePosition = false;
                    mChangeBrightness = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i(TAG, "onTouch surfaceContainer actionMove [" + this.hashCode() + "] ");
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                cancelProgressTimer();
                                if (absDeltaX >= THRESHOLD) {
                                    // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                                    // 否则会因为mediaplayer的状态非法导致App Crash
                                    if (currentState != CURRENT_STATE_ERROR) {
                                        mChangePosition = true;
                                        mGestureDownPosition = getCurrentPositionWhenPlaying();
                                    }
                                } else {
                                    //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                                    if (mDownX < mScreenWidth * 0.5f) {//左侧改变亮度
                                        mChangeBrightness = true;
                                        WindowManager.LayoutParams lp = JZUtils.getWindow(getContext()).getAttributes();
                                        if (lp.screenBrightness < 0) {
                                            try {
                                                mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                                                Log.i(TAG, "current system brightness: " + mGestureDownBrightness);
                                            } catch (Settings.SettingNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            mGestureDownBrightness = lp.screenBrightness * 255;
                                            Log.i(TAG, "current activity brightness: " + mGestureDownBrightness);
                                        }
                                    } else {//右侧改变声音
                                        mChangeVolume = true;
                                        mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    }
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        long totalTimeDuration = getDuration();
                        mSeekTimePosition = (int) (mGestureDownPosition + deltaX * totalTimeDuration / mScreenWidth);
                        if (mSeekTimePosition > totalTimeDuration)
                            mSeekTimePosition = totalTimeDuration;
                        String seekTime = JZUtils.stringForTime(mSeekTimePosition);
                        String totalTime = JZUtils.stringForTime(totalTimeDuration);

                        showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY;
                        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                        //dialog中显示百分比
                        int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);
                        showVolumeDialog(-deltaY, volumePercent);
                    }

                    if (mChangeBrightness) {
                        deltaY = -deltaY;
                        int deltaV = (int) (255 * deltaY * 3 / mScreenHeight);
                        WindowManager.LayoutParams params = JZUtils.getWindow(getContext()).getAttributes();
                        if (((mGestureDownBrightness + deltaV) / 255) >= 1) {//这和声音有区别，必须自己过滤一下负值
                            params.screenBrightness = 1;
                        } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
                            params.screenBrightness = 0.01f;
                        } else {
                            params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
                        }
                        JZUtils.getWindow(getContext()).setAttributes(params);
                        //dialog中显示百分比
                        int brightnessPercent = (int) (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight);
                        showBrightnessDialog(brightnessPercent);
//                        mDownY = y;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "onTouch surfaceContainer actionUp [" + this.hashCode() + "] ");
                    mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumeDialog();
                    dismissBrightnessDialog();
                    if (mChangePosition) {
                        onEvent(JZUserAction.ON_TOUCH_SCREEN_SEEK_POSITION);
                        mediaInterface.seekTo(mSeekTimePosition);
                        long duration = getDuration();
                        int progress = (int) (mSeekTimePosition * 100 / (duration == 0 ? 1 : duration));
                        progressBar.setProgress(progress);
                    }
                    if (mChangeVolume) {
                        onEvent(JZUserAction.ON_TOUCH_SCREEN_SEEK_VOLUME);
                    }
                    startProgressTimer();
                    break;
            }
        }
        return false;
    }

    public static void setCurrentJzvd(Jzvd jzvd) {
        if (CURRENT_JZVD != null) CURRENT_JZVD.reset();
        CURRENT_JZVD = jzvd;
    }

    public void startVideo() {
        Log.d(TAG, "startVideo [" + this.hashCode() + "] ");
        setCurrentJzvd(this);
        addTextureView();
        AudioManager mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        JZUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        onStatePreparing();
    }

    public void onPrepared() {
        Log.i(TAG, "onPrepared " + " [" + this.hashCode() + "] ");
        onStatePrepared();
        onStatePlaying();
    }

    public void setState(int state) {
        setState(state, 0, 0);
    }

    public void setState(int state, int urlMapIndex, int seekToInAdvance) {//后面两个参数干嘛的
        switch (state) {
            case CURRENT_STATE_NORMAL:
                onStateNormal();
                break;
            case CURRENT_STATE_PREPARING:
                onStatePreparing();
                break;
            case CURRENT_STATE_PREPARING_CHANGING_URL:
                changeUrl(urlMapIndex, seekToInAdvance);
                break;
            case CURRENT_STATE_PLAYING:
                onStatePlaying();
                break;
            case CURRENT_STATE_PAUSE:
                onStatePause();
                break;
            case CURRENT_STATE_ERROR:
                onStateError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                onStateAutoComplete();
                break;
        }
    }

    public void setScreen(int screen) {//特殊的个别的进入全屏的按钮在这里设置  只有setup的时候能用上
        switch (screen) {
            case SCREEN_WINDOW_NORMAL:
                setScreenNormal();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setScreenFullscreen();
                break;
            case SCREEN_WINDOW_TINY:
                setScreenTiny();
                break;
        }
    }

    public void onStateNormal() {
        Log.i(TAG, "onStateNormal " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_NORMAL;
        cancelProgressTimer();
    }

    public void onStatePreparing() {
        Log.i(TAG, "onStatePreparing " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PREPARING;
        resetProgressAndTime();
    }

    public void changeUrl(int urlMapIndex, long seekToInAdvance) {
        currentState = CURRENT_STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        jzDataSource.currentUrlIndex = urlMapIndex;
        mediaInterface.prepare();
    }

    public void changeUrl(JZDataSource jzDataSource, long seekToInAdvance) {
        currentState = CURRENT_STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        this.jzDataSource = jzDataSource;
//        if (JzvdMgr.getSecondFloor() != null && JzvdMgr.getFirstFloor() != null) {
//            JzvdMgr.getFirstFloor().jzDataSource = jzDataSource;
//        }
        mediaInterface.prepare();
    }

    public void changeUrl(String url, String title, long seekToInAdvance) {
        changeUrl(new JZDataSource(url, title), seekToInAdvance);
    }

    public void onStatePrepared() {//因为这个紧接着就会进入播放状态，所以不设置state
        if (seekToInAdvance != 0) {
            mediaInterface.seekTo(seekToInAdvance);
            seekToInAdvance = 0;
        } else {
            long position = JZUtils.getSavedProgress(getContext(), jzDataSource.getCurrentUrl());
            if (position != 0) {
                mediaInterface.seekTo(position);
            }
        }
    }

    public void onStatePlaying() {
        Log.i(TAG, "onStatePlaying " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PLAYING;
        startProgressTimer();
    }

    public void onStatePause() {
        Log.i(TAG, "onStatePause " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PAUSE;
        startProgressTimer();
    }

    public void onStateError() {
        Log.i(TAG, "onStateError " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_ERROR;
        cancelProgressTimer();
    }

    public void onStateAutoComplete() {
        Log.i(TAG, "onStateAutoComplete " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_AUTO_COMPLETE;
        cancelProgressTimer();
        progressBar.setProgress(100);
        currentTimeTextView.setText(totalTimeTextView.getText());
    }

    public void onInfo(int what, int extra) {
        Log.d(TAG, "onInfo what - " + what + " extra - " + extra);
    }

    public void onError(int what, int extra) {
        Log.e(TAG, "onError " + what + " - " + extra + " [" + this.hashCode() + "] ");
        if (what != 38 && extra != -38 && what != -38 && extra != 38 && extra != -19) {
            onStateError();
            mediaInterface.release();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN || currentScreen == SCREEN_WINDOW_TINY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthRatio != 0 && heightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) heightRatio) / widthRatio);
            setMeasuredDimension(specWidth, specHeight);

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    public void onAutoCompletion() {
        Runtime.getRuntime().gc();
        Log.i(TAG, "onAutoCompletion " + " [" + this.hashCode() + "] ");
        onEvent(JZUserAction.ON_AUTO_COMPLETE);
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateAutoComplete();

//        if (currentScreen == SCREEN_WINDOW_FULLSCREEN || currentScreen == SCREEN_WINDOW_TINY) {
//            backPress();
//        }
        mediaInterface.release();
        JZUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        JZUtils.saveProgress(getContext(), jzDataSource.getCurrentUrl(), 0);
    }

    public void reset() {
        Log.i(TAG, "reset " + " [" + this.hashCode() + "] ");
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            long position = getCurrentPositionWhenPlaying();
            JZUtils.saveProgress(getContext(), jzDataSource.getCurrentUrl(), position);
        }
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateNormal();
        textureViewContainer.removeAllViews();
        JZMediaInterface.SAVED_SURFACE = null;

        AudioManager mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        JZUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        JZUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
        mediaInterface.release();
    }

//    public void release() {
//        if (jzDataSource.getCurrentUrl().equals(JZMediaPlayer.getCurrentUrl()) &&
//                (System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
//            //在非全屏的情况下只能backPress()
//            if (JzvdMgr.getSecondFloor() != null &&
//                    JzvdMgr.getSecondFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//点击全屏
//            } else if (JzvdMgr.getSecondFloor() == null && JzvdMgr.getFirstFloor() != null &&
//                    JzvdMgr.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//直接全屏
//            } else {
//                Log.d(TAG, "releaseMediaPlayer [" + this.hashCode() + "]");
//                resetAllVideos();
//            }
//        }
//    }

    JZTextureView textureView;

    public void addTextureView() {
        Log.d(TAG, "addTextureView [" + this.hashCode() + "] ");
        if (textureView != null) textureViewContainer.removeView(textureView);
        textureView = new JZTextureView(getContext().getApplicationContext());
        textureView.setSurfaceTextureListener(mediaInterface);

        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        textureViewContainer.addView(textureView, layoutParams);
    }

    public void clearFloatScreen() {
//        JZUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
////        showStatusBar(getContext());
//        ViewGroup vp = (JZUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
//                .findViewById(Window.ID_ANDROID_CONTENT);
//        Jzvd fullJzvd = vp.findViewById(R.id.jz_fullscreen_id);
//        Jzvd tinyJzvd = vp.findViewById(R.id.jz_tiny_id);
//
//        if (fullJzvd != null) {
//            vp.removeView(fullJzvd);
//        }
//        if (tinyJzvd != null) {
//            vp.removeView(tinyJzvd);
//        }
//        JzvdMgr.setSecondFloor(null);
    }

    public void onVideoSizeChanged(int width, int height) {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (textureView != null) {
            if (videoRotation != 0) {
                textureView.setRotation(videoRotation);
            }
            textureView.setVideoSize(width, height);
        }
    }

    public void startProgressTimer() {
        Log.i(TAG, "startProgressTimer: " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public void onProgress(int progress, long position, long duration) {
//        Log.d(TAG, "onProgress: progress=" + progress + " position=" + position + " duration=" + duration);
        if (!mTouchingProgressBar) {
            if (seekToManulPosition != -1) {
                if (seekToManulPosition > progress) {
                    return;
                } else {
                    seekToManulPosition = -1;
                }
            } else {
                if (progress != 0) progressBar.setProgress(progress);
            }
        }
        if (position != 0) currentTimeTextView.setText(JZUtils.stringForTime(position));
        totalTimeTextView.setText(JZUtils.stringForTime(duration));
    }

    public void setBufferProgress(int bufferProgress) {
        if (bufferProgress != 0) progressBar.setSecondaryProgress(bufferProgress);
    }

    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(JZUtils.stringForTime(0));
        totalTimeTextView.setText(JZUtils.stringForTime(0));
    }

    public long getCurrentPositionWhenPlaying() {
        long position = 0;
        //TODO 这块的判断应该根据MediaPlayer来
        if (currentState == CURRENT_STATE_PLAYING ||
                currentState == CURRENT_STATE_PAUSE) {
            try {
                position = mediaInterface.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    public long getDuration() {
        long duration = 0;
        //TODO MediaPlayer 判空的问题
//        if (JZMediaPlayer.instance().mediaPlayer == null) return duration;
        try {
            duration = mediaInterface.getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        onEvent(JZUserAction.ON_SEEK_POSITION);
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (currentState != CURRENT_STATE_PLAYING &&
                currentState != CURRENT_STATE_PAUSE) return;
        long time = seekBar.getProgress() * getDuration() / 100;
        seekToManulPosition = seekBar.getProgress();
        mediaInterface.seekTo(time);
        Log.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    public int seekToManulPosition = -1;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            //设置这个progres对应的时间，给textview
            long duration = getDuration();
            currentTimeTextView.setText(JZUtils.stringForTime(progress * duration / 100));
        }
    }


    public void startWindowFullscreen() {
        ViewGroup vp = (ViewGroup) CURRENT_JZVD.getParent();
        vp.removeView(CURRENT_JZVD);
        CONTAINER_LIST.add(vp);

        ViewGroup vg = (ViewGroup) (JZUtils.scanForActivity(getContext())).getWindow().getDecorView();//和他也没有关系
        vg.addView(CURRENT_JZVD, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        JZUtils.hideStatusBar(getContext());

        setScreenFullscreen();
//        hideSystemUI((JZUtils.scanForActivity(getContext())).getWindow().getDecorView());
//            jzvd.setUp(jzDataSource, JzvdStd.SCREEN_WINDOW_FULLSCREEN);//华为手机和有虚拟键的手机全屏时可隐藏虚拟键 issue:1326

    }

    public void setScreenNormal() {
        currentScreen = SCREEN_WINDOW_NORMAL;
    }

    public void setScreenFullscreen() {
        currentScreen = SCREEN_WINDOW_FULLSCREEN;
    }

    public void setScreenTiny() {
        currentScreen = SCREEN_WINDOW_TINY;
    }

    //下面还有onStete...  从MediaPlayer回调过来的


//    //重力感应的时候调用的函数，、、这里有重力感应的参数，暂时不能删除
//    public void autoFullscreen(float x) {
//        if (isCurrentPlay()
//                && (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE)
//                && currentScreen != SCREEN_WINDOW_FULLSCREEN
//                && currentScreen != SCREEN_WINDOW_TINY) {
//            if (x > 0) {
//                JZUtils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            } else {
//                JZUtils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
//            }
//            onEvent(JZUserAction.ON_ENTER_FULLSCREEN);
//            startWindowFullscreen();
//        }
//    }
//
//    public void autoQuitFullscreen() {
//        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
//                && isCurrentPlay()
//                && currentState == CURRENT_STATE_PLAYING
//                && currentScreen == SCREEN_WINDOW_FULLSCREEN) {
//            lastAutoFullscreenTime = System.currentTimeMillis();
//            backPress();
//        }
//    }

    public void onEvent(int type) {
        if (JZ_USER_EVENT != null && !jzDataSource.urlsMap.isEmpty()) {
            JZ_USER_EVENT.onEvent(type, jzDataSource.getCurrentUrl(), currentScreen);
        }
    }

    //TODO 是否有用
    public void onSeekComplete() {

    }

    public void showWifiDialog() {
    }

    public void showProgressDialog(float deltaX,
                                   String seekTime, long seekTimePosition,
                                   String totalTime, long totalTimeDuration) {
    }

    public void dismissProgressDialog() {

    }

    public void showVolumeDialog(float deltaY, int volumePercent) {

    }

    public void dismissVolumeDialog() {

    }

    public void showBrightnessDialog(int brightnessPercent) {

    }

    public void dismissBrightnessDialog() {

    }

    public static class JZAutoFullscreenListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {//可以得到传感器实时测量出来的变化值
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            //过滤掉用力过猛会有一个反向的大数值
            if (x < -12 || x > 12) {
                if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
//                  JzvdMgr.getCurrentJzvd().autoFullscreen(x);
                    lastAutoFullscreenTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
//                Log.v(TAG, "onProgressUpdate " + "[" + this.hashCode() + "] ");
                post(() -> {
                    long position = getCurrentPositionWhenPlaying();
                    long duration = getDuration();
                    int progress = (int) (position * 100 / (duration == 0 ? 1 : duration));
                    onProgress(progress, position, duration);
                });
            }
        }
    }

    public Context getApplicationContext() {
        Context context = getContext();
        if (context != null) {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext != null) {
                return applicationContext;
            }
        }
        return context;
    }
}
