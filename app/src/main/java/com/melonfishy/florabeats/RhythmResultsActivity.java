package com.melonfishy.florabeats;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import pl.droidsonroids.gif.GifImageView;

public class RhythmResultsActivity extends AppCompatActivity {

    public static final String BLOOM_COUNT_EXTRA = "bloomCount";
    public static final String GROW_COUNT_EXTRA = "growCount";
    public static final String MISS_COUNT_EXTRA = "missCount";
    public static final String SCORE_EXTRA = "score";
    public static final String MAX_COMBO_EXTRA = "maxCombo";
    public static final String SUCCESS_EXTRA = "success";
    public static final int ANIM_DURATION = 1800;
    public static final int FADE_DURATION = 200;
    public static final int JINGLE_DURATION = 3000;
    private static final String TAG = RhythmResultsActivity.class.getSimpleName();

    private int mCounter = 0;
    private int mBlooms, mGrows, mMisses, mMaxCombo, length;
    private String mScore;
    private TextView mBloomCount, mGrowCount, mMissCount, mScoreText, mClearMessage, mMaxComboText;
    private FrameLayout mAnimFrame, mWholeFrame;
    private GifImageView mClearAnim, mClearAnimEx;
    private ImageView mClearFail;
    private Timer mTimer;
    private MediaPlayer mPlayer, mNextPlayer;
    private boolean success = false;
    private boolean animFinished = false;
    private boolean canFade = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        setContentView(R.layout.activity_rhythm_results);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorLightBlack));
        }

        if (getIntent() != null) {
            if (getIntent().hasExtra(BLOOM_COUNT_EXTRA)) {
                mBlooms = getIntent().getIntExtra(BLOOM_COUNT_EXTRA, 0);
            }
            if (getIntent().hasExtra(GROW_COUNT_EXTRA)) {
                mGrows = getIntent().getIntExtra(GROW_COUNT_EXTRA, 0);
            }
            if (getIntent().hasExtra(MISS_COUNT_EXTRA)) {
                mMisses = getIntent().getIntExtra(MISS_COUNT_EXTRA, 0);
            }
            if (getIntent().hasExtra(SCORE_EXTRA)) {
                mScore = getIntent().getStringExtra(SCORE_EXTRA);
            }
            if (getIntent().hasExtra(MAX_COMBO_EXTRA)) {
                mMaxCombo = getIntent().getIntExtra(MAX_COMBO_EXTRA, 0);
            }
            if (getIntent().hasExtra(SUCCESS_EXTRA)) {
                success = getIntent().getBooleanExtra(SUCCESS_EXTRA, false);
            }
        }

        length = 0;
        mBloomCount = (TextView) findViewById(R.id.tv_bloom_count);
        mBloomCount.setText(String.valueOf(mBlooms));
        mGrowCount = (TextView) findViewById(R.id.tv_grow_count);
        mGrowCount.setText(String.valueOf(mGrows));
        mMissCount = (TextView) findViewById(R.id.tv_miss_count);
        mMissCount.setText(String.valueOf(mMisses));
        mScoreText = (TextView) findViewById(R.id.tv_score_count);
        mScoreText.setText(mScore);
        mClearFail = (ImageView) findViewById(R.id.iv_rhythm_clear_fail);
        mMaxComboText = (TextView) findViewById(R.id.tv_max_combo_count);
        mMaxComboText.setText(String.valueOf(mMaxCombo));
        mTimer = new Timer();

        mClearMessage = (TextView) findViewById(R.id.tv_rhythm_clear_message);
        mAnimFrame = (FrameLayout) findViewById(R.id.fl_result_anim);
        mClearAnim = (GifImageView) findViewById(R.id.giv_rhythm_clear_anim);
        mClearAnimEx = (GifImageView) findViewById(R.id.giv_rhythm_clear_anim_ex);
        mWholeFrame = (FrameLayout) findViewById(R.id.fl_result_data);

        if (!success) {
            mClearFail.setVisibility(View.VISIBLE);
            mClearAnim.setVisibility(View.INVISIBLE);
            mClearMessage.setText(getResources().getString(R.string.rhythm_clear_fail));
            mAnimFrame.setBackground(ContextCompat.getDrawable(this,
                    R.color.colorSongFailure));
            mPlayer = MediaPlayer.create(this, R.raw.song_clear_jingle_failure);
            mPlayer.start();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mPlayer.release();
                    animFinished = true;
                    mPlayer = MediaPlayer.create(RhythmResultsActivity.this,
                            R.raw.song_clear_results_failure);
                    mPlayer.setVolume(0f, 0f);
                    mPlayer.start();
                    startFadeIn();
                    mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mPlayer.start();
                            if (canFade) {
                                startFadeIn();
                                canFade = false;
                            }
                        }
                    });
                    createNextMediaPlayer();
                }
            }, JINGLE_DURATION);
        } else {
            mPlayer = MediaPlayer.create(this, R.raw.song_clear_jingle_success);
            mPlayer.start();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mPlayer.release();
                    animFinished = true;
                    mPlayer = MediaPlayer.create(RhythmResultsActivity.this,
                            R.raw.song_clear_results_success);
                    mPlayer.setVolume(0f, 0f);
                    mPlayer.start();
                    startFadeIn();
                    canFade = true;
                    mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mPlayer.start();
                            if (canFade) {
                                startFadeIn();
                                canFade = false;
                            }
                        }
                    });
                    createNextMediaPlayer();
                }
            }, JINGLE_DURATION);
            if (mMisses == 0) {
                mClearAnimEx.setVisibility(View.VISIBLE);
                mClearAnim.setVisibility(View.INVISIBLE);
                if (mGrows == 0) {
                    mClearMessage.setText(getResources().getString(R.string.rhythm_clear_bloom));
                    mAnimFrame.setBackground(ContextCompat.getDrawable(this,
                            R.color.colorSongClearBloom));
                } else {
                    mClearMessage.setText(getResources().getString(R.string.rhythm_clear_grow));
                    mAnimFrame.setBackground(ContextCompat.getDrawable(this,
                            R.color.colorSongClearGrow));
                }
            }
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AnimatorSet fader = (AnimatorSet) AnimatorInflater.loadAnimator(
                                RhythmResultsActivity.this, R.animator.simple_fade);
                        fader.setDuration(FADE_DURATION);
                        fader.setTarget(mAnimFrame);
                        fader.start();
                    }
                });
            }
        }, ANIM_DURATION);

        mWholeFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (animFinished) {
                    Intent intent = new Intent(RhythmResultsActivity.this,
                            SongNavigationActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, SongNavigationActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            length = mPlayer.getCurrentPosition();
        }
    }

    private int getCurrentSong() {
        if (success) {
            if (!animFinished) {
                return R.raw.song_clear_jingle_success;
            } else {
                return R.raw.song_clear_results_success;
            }
        } else {
            if (!animFinished) {
                return R.raw.song_clear_jingle_failure;
            } else {
                return R.raw.song_clear_results_failure;
            }
        }
    }

    @Override
    protected void onRestart() {
        canFade = true;
        super.onRestart();
        mPlayer = MediaPlayer.create(this, getCurrentSong());
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mPlayer.start();
                if (canFade) {
                    startFadeIn();
                    canFade = false;
                }
            }
        });
        mPlayer.seekTo(length);
        mPlayer.start();

        createNextMediaPlayer();
    }

    @Override
    protected void onStop() {
        mPlayer.release();
        super.onStop();
    }

    private float volume;
    private void startFadeIn(){
        final int FADE_DURATION = 500;
        final int FADE_INTERVAL = 25;
        final int MAX_VOLUME = 1;
        int numberOfSteps = FADE_DURATION/FADE_INTERVAL;
        final float deltaVolume = MAX_VOLUME / (float)numberOfSteps;
        volume = 0f;

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if(volume>=1f){
                    return;
                }
                fadeInStep(deltaVolume); //Do a fade step
                //Cancel and Purge the Timer if the desired volume has been reached
            }
        };

        mTimer.schedule(timerTask,FADE_INTERVAL,FADE_INTERVAL);
    }

    private void fadeInStep(float deltaVolume){
        mPlayer.setVolume(volume, volume);
        volume += deltaVolume;
    }

    private void createNextMediaPlayer() {
        mNextPlayer = MediaPlayer.create(this, getCurrentSong());
        mPlayer.setNextMediaPlayer(mNextPlayer);
        mPlayer.setOnCompletionListener(onCompletionListener);
    }

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.release();
            mPlayer = mNextPlayer;

            createNextMediaPlayer();

            Log.d(TAG, String.format("Loop #%d", ++mCounter));
        }
    };
}
