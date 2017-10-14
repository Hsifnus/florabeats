package com.melonfishy.florabeats;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Debug;
import android.os.PersistableBundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.melonfishy.florabeats.data.RhythmPlant;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import layout.FlowerFragment;

public class RhythmPlantActivity extends AppCompatActivity {

    private static final int LINE_DRAWING_OFFSET = 50;
    private static final int LOADING_OFFSET_TIME = 2000;
    private static final int PAUSE_LOAD_OFFSET_TIME = 100;
    private static final int BLOOM_IN_OFFSET_TIME = 500;
    private static final int READY_SIGNAL_OFFSET_TIME = 1000;
    private int signalSendCooldown = 50;
    private static final double[] signalSendCooldownWeights = new double[]{2.0, 2.0, 2.5, 2.5};

    private static final int BLOOM_TIMING_WINDOW = 100;
    private static final int GROW_TIMING_WINDOW = 220;
    private static final int MISS_TIMING_WINDOW = 500;

    public static final int START_SONG_REQUEST = 800;
    public static final int SONG_CLEAR_RESULT = 900;
    public static final int SONG_CANCEL_RESULT = 902;
    public static final int SONG_FAIL_RESULT = 901;

    public static final int SONG_MISS_INDEX = 0;
    public static final int SONG_NEAR_MISS_INDEX = 1;
    public static final int SONG_GROW_INDEX = 2;
    public static final int SONG_BLOOM_INDEX = 3;

    public static final String RHYTHM_SONGFILE = "rhythmSongfile";

    public static boolean AUTO_ASSIST = false;

    LinearLayout tier1, tier2, tier3, tier4, comboLayer, difficultyStars;
    FrameLayout vineLayer, fxLayer, fxLayer2, pauseLayer;
    ConstraintLayout layoutArea;
    HashMap<RhythmPlant.PlantNode, FlowerFragment> nodeFragmentBindings;
    HashMap<RhythmPlant.PlantNode, FrameLayout> nodeLayoutBindings;
    HashMap<RhythmPlant.PlantNode, LineView> nodeLineBindings;
    HashMap<Integer, ArrayList<RhythmTrack.Step>> fragmentStepBindings;
    double[] nodeTimes;
    RhythmPlant mPlant;
    Timer mTimer, mSFXTimerA, mSFXTimerB, mSFXTimerC;
    ArrayList<Timer> sfxTimers;
    Button resumeButton, quitButton;
    HashMap<String, ArrayList<Integer>> difficultyJudgeWeights;
    static MediaPlayer mPlayer;
    static SoundPool mHitSound;
    private int songID, sfxIndex;
    private int[] hitSoundIDs;
    RhythmTrack mTrack;
    TextView mReadyMessage, mCombo, mComboLabel, mScoreLabel, mDifficultyLabel, mLoadingText;
    SparseIntArray currentBindings;
    ImageView mDifficultyIcon, mLoadingImage;
    TrackTask mTrackTask = new TrackTask();
    private static String filename = "001_morning_sprout_F.txt";
    private Drawable sparkGraphic;
    private Drawable signalGraphic;
    boolean startFlag = false;
    boolean finishFlag = false;
    boolean hasResumed = false;
    boolean comboAnimating = false;

    int combo, notesEaten, bloomCount, growCount, missCount, maxCombo;
    private static int[] currentNodeStatus;
    private static int savedCombo = 0;
    private static int totalNotes = 0;
    private static int noteProgress = 0;
    private static int savedEaten = 0;
    double score;
    private static double savedScore = 0;
    private static double savedTimerOffset = 0.0;
    private boolean pauseFlag = false;
    private boolean unpauseFlag = true;

    private static final String SAVED_SCORE = "savedScore";
    private static final String SAVED_TIMER_OFFSET = "savedTimerOffset";
    private static final String SAVED_NOTE_PROGRESS = "noteProgress";
    private static final String SAVED_COMBO = "savedCombo";
    private static final String SAVED_EATEN = "savedEaten";
    private static final String SAVED_NODE_STATUS = "savedNodeStatus";

    boolean comboActivated;

    private Animator.AnimatorListener comboAnimationListener() {
        return new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                comboAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                comboAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };
    }
    private Animator.AnimatorListener mComboListener = comboAnimationListener();

    private class HitSoundTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            mHitSound.play(hitSoundIDs[sfxIndex], 1, 1, 1, 0, 1f);
            return null;
        }
    }

    private class TrackTask extends AsyncTask<Void, Void, Void> {

        private boolean isRunning = false;
        @Override
        protected Void doInBackground(Void... params) {
            mTrack = new RhythmTrack(filename, RhythmPlantActivity.this);
            totalNotes = mTrack.getNoteCount();

            String plantData = mTrack.getPlantData();

            String audioFile = mTrack.getAudioname();
            int audioRes = getResources().getIdentifier(audioFile, "raw", RhythmPlantActivity.this
            .getPackageName());
            mPlayer = MediaPlayer.create(RhythmPlantActivity.this, audioRes);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                SoundPool.Builder builder = new SoundPool.Builder();
                mHitSound = builder.setMaxStreams(31)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setLegacyStreamType(AudioManager.STREAM_MUSIC).build()).build();
            } else {
                mHitSound = new SoundPool(31, AudioManager.STREAM_MUSIC, 0);
            }
            hitSoundIDs = new int[]{mHitSound.load(RhythmPlantActivity.this, R.raw.auto_hitsound, 1),
                    mHitSound.load(RhythmPlantActivity.this, R.raw.auto_hitsound, 1),
                    mHitSound.load(RhythmPlantActivity.this, R.raw.auto_hitsound, 1)};
            songID = mHitSound.load(RhythmPlantActivity.this, audioRes, 1);
            mHitSound.play(hitSoundIDs[1], 0, 0, 1, -1, 1f);

            mPlant = new RhythmPlant(plantData);
            currentNodeStatus = new int[mPlant.getSize()];
            nodeTimes = new double[mPlant.getSize()];
            for (int i = 0; i < mPlant.getSize(); i++) {
                nodeTimes[i] = 0.0;
                currentNodeStatus[i] = 0;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
//            Log.d("test", isRunning + "");
            if (isRunning) {
                return;
            } else {
                isRunning = true;
            }
            ArrayDeque<RhythmPlant.PlantNode> nodeBuffer = new ArrayDeque<>();
            setScore(0);

            for (int i = 0; i < mPlant.getSize(); i++) {
                FrameLayout frame = new FrameLayout(RhythmPlantActivity.this);
                int frameID = mPlant.getHash(i);
                frame.setId(frameID);

                int level = mPlant.getLevel(i);
                FlowerFragment f = FlowerFragment.newInstance(i, RhythmPlant.STARTING_HEALTH, level);
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.add(frameID, f).commit();
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int)
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                                getResources().getDisplayMetrics()),
                        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
                frame.setLayoutParams(params);

                if (level == 1) {
                    addViewToTier(1, frame);
                    nodeBuffer.add(mPlant.getNode(i));
                    nodeLayoutBindings.put(mPlant.getNode(i), frame);
                    nodeFragmentBindings.put(mPlant.getNode(i), f);
                }
            }

            HashMap<Integer, Integer> levelHistogram = mPlant.generateLevelHistogram();

            while (!nodeBuffer.isEmpty()) {
                RhythmPlant.PlantNode node = nodeBuffer.removeFirst();

                for (RhythmPlant.PlantNode child : node.getChildren()) {
                    int i = child.getID();
                    FrameLayout frame = new FrameLayout(RhythmPlantActivity.this);
                    int frameID = mPlant.getHash(i);
                    frame.setId(frameID);

                    int level = mPlant.getLevel(i);

                    FlowerFragment f = FlowerFragment.newInstance(i, RhythmPlant.STARTING_HEALTH, level);
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.add(frameID, f).commit();
                    int nodeWidth = Math.max(360 / levelHistogram.get(level), 90);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int)
                            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, nodeWidth,
                                    getResources().getDisplayMetrics()),
                            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
                    frame.setLayoutParams(params);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        frame.setElevation(10.0f);
                    }
                    addViewToTier(level, frame);
                    nodeBuffer.add(child);
                    nodeLayoutBindings.put(child, frame);
                    nodeFragmentBindings.put(child, f);
                }
            }
//            Log.d("RhythmPlantActivity", "" + levelHistogram.get(1));
//            Log.d("RhythmPlantActivity", "" + levelHistogram.get(2));
//            Log.d("RhythmPlantActivity", "" + levelHistogram.get(3));

            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        for (int i = 0; i < mPlant.getSize(); i++) {
                            RhythmPlant.PlantNode child = mPlant.getNode(i);
                            if (child.getParent() != null) {
                                RhythmPlant.PlantNode node = child.getParent();
                                int[] childCoords = new int[2];
                                nodeLayoutBindings.get(child).getLocationOnScreen(childCoords);

                                int[] parentCoords = new int[2];
                                nodeLayoutBindings.get(node).getLocationOnScreen(parentCoords);

                                childCoords[0] += nodeLayoutBindings.get(child).getWidth() / 2;
                                childCoords[1] += (int)
                                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45,
                                                getResources().getDisplayMetrics());

                                parentCoords[0] += nodeLayoutBindings.get(node).getWidth() / 2;
                                parentCoords[1] += (int)
                                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45,
                                                getResources().getDisplayMetrics());

                                addLine(Math.round(childCoords[0]),
                                        Math.round(childCoords[1]),
                                        Math.round(parentCoords[0]),
                                        Math.round(parentCoords[1]),
                                        child.getID(), node.getID());
                            } else {
                                int[] childCoords = new int[2];
                                nodeLayoutBindings.get(child).getLocationOnScreen(childCoords);

                                int[] parentCoords = new int[]{childCoords[0],
                                        layoutArea.getHeight()};

                                childCoords[0] += nodeLayoutBindings.get(child).getWidth() / 2;
                                childCoords[1] += (int)
                                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45,
                                                getResources().getDisplayMetrics());

                                parentCoords[0] += nodeLayoutBindings.get(child).getWidth() / 2;

                                addLine(Math.round(childCoords[0]),
                                        Math.round(childCoords[1]),
                                        Math.round(parentCoords[0]),
                                        Math.round(parentCoords[1]),
                                        child.getID(), -1);
                            }
                            toggleFlowerVisibility(i);
                        }
                        provideSongInfo();
                        }
                    });
                }
            }, LINE_DRAWING_OFFSET);

            deployRhythmTrack();
            isRunning = false;
            super.onPostExecute(aVoid);
        }
    }

    private class SparkTask extends AsyncTask<Integer, Void, Integer[]> {

        FrameLayout focus;
        int[] targetCoords;
        FrameLayout.LayoutParams params;
        int flowerSideLength;
        Integer nodeID, mode;
        ImageView newView;
        Context context;

        @Override
        protected Integer[] doInBackground(Integer... vals) {
            context = RhythmPlantActivity.this;
            nodeID = vals[0];
            mode = vals[1];
            targetCoords = new int[2];
            focus = nodeLayoutBindings.get(mPlant.getNode(nodeID));
            params = new FrameLayout.LayoutParams((int)
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                            getResources().getDisplayMetrics()), (int)
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                            getResources().getDisplayMetrics()));
            flowerSideLength = (int)
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                            getResources().getDisplayMetrics());
            return null;
        }

        @Override
        protected void onPostExecute(Integer[] vals) {
            newView = new ImageView(context);
            targetCoords[0] += (nodeLayoutBindings.get(mPlant.getNode(nodeID)).getWidth()
                    - flowerSideLength) / 2;
            targetCoords[1] += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                    getResources().getDisplayMetrics());
            focus.getLocationOnScreen(targetCoords);
            newView.setImageDrawable(sparkGraphic);

            targetCoords[0] += (nodeLayoutBindings.get(mPlant.getNode(nodeID)).getWidth()
                    - flowerSideLength) / 2;
            targetCoords[1] += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                    getResources().getDisplayMetrics());

            params.setMargins(targetCoords[0], targetCoords[1], 0, 0);
            newView.setLayoutParams(params);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mode == FlowerFragment.NORMAL_SPARK) {
                    newView.setImageTintList(ContextCompat.getColorStateList(context,
                            R.color.colorSparkGrow));
                } else if (mode == FlowerFragment.BLOOM_SPARK) {
                    newView.setImageTintList(ContextCompat.getColorStateList(context,
                            R.color.colorSparkBloom));
                } else if (mode == FlowerFragment.SPROUT_SPARK) {
                    newView.setImageTintList(ContextCompat.getColorStateList(context,
                            R.color.colorSparkSprout));
                } else if (mode == FlowerFragment.WITHER_SPARK) {
                    newView.setImageTintList(ContextCompat.getColorStateList(context,
                            R.color.colorSparkWither));
                }
            }
            fxLayer2.addView(newView);
            AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(context,
                    R.animator.spark_burst);
            set.setDuration(millisFromBPM(240));
            set.setTarget(newView);
            set.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    newView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
//                    Log.d(RhythmPlantActivity.class.getSimpleName(), "fxLayer2 size: " + fxLayer2.getChildCount());
                    fxLayer2.removeView(newView);
                    newView.post(new Runnable() {
                        @Override
                        public void run() {
                            newView.setLayerType(View.LAYER_TYPE_NONE, null);
                        }
                    });
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            set.start();
        }
    }

    private class SignalTask extends AsyncTask<Object, Void, Object[]> {
        Context context;
        ImageView newView;
        FrameLayout focus;
        int nodeID, flowerSideLength, BPM;
        int[] targetCoords;
        RhythmTrack.Step origin;
        boolean flag_signal, flag_judge;
        FrameLayout.LayoutParams layoutParams;

        protected Object[] doInBackground(Object... params) {
            nodeID = (Integer) params[0];
            BPM = (Integer) params[1];
            origin = (RhythmTrack.Step) params[2];
            context = RhythmPlantActivity.this;
            focus = nodeLayoutBindings.get(mPlant.getNode(nodeID));
            targetCoords = new int[2];
            layoutParams = new FrameLayout.LayoutParams((int)
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                            getResources().getDisplayMetrics()), (int)
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                            getResources().getDisplayMetrics()));
            flowerSideLength = (int)
                    (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                            getResources().getDisplayMetrics()));
            return new Object[0];
        }

        @Override
        protected void onPostExecute(Object[] objects) {
            newView = new ImageView(context);
            focus.getLocationOnScreen(targetCoords);
            newView.setImageDrawable(signalGraphic);
            targetCoords[0] += (nodeLayoutBindings.get(mPlant.getNode(nodeID)).getWidth()
                    - flowerSideLength) / 2;
            targetCoords[1] += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                    getResources().getDisplayMetrics());

            layoutParams.setMargins(targetCoords[0], targetCoords[1], 0, 0);
            newView.setLayoutParams(layoutParams);

            final long timing_miss = millisFromBPM(BPM, 2) + MISS_TIMING_WINDOW;
            final long timing_signal = millisFromBPM(BPM, 2.3);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                newView.setImageTintList(ContextCompat.getColorStateList(context, R.color.colorSignal1));
            }
            fxLayer.addView(newView);
            AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(context,
                    R.animator.signal_shrink);
            set.setDuration(millisFromBPM(BPM, 2.3));
            set.setTarget(newView);
            flag_signal = timing_signal > timing_miss;
            set.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    newView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (flag_signal || AUTO_ASSIST) {
//                        Log.d(RhythmPlantActivity.class.getSimpleName(), "fxLayer size: " + fxLayer.getChildCount());
                        fxLayer.removeView(newView);
                    }
                    newView.post(new Runnable() {
                        @Override
                        public void run() {
                            newView.setLayerType(View.LAYER_TYPE_NONE, null);
                        }
                    });
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            if (AUTO_ASSIST) {
                AnimatorSet set2 = (AnimatorSet) AnimatorInflater.loadAnimator(context,
                        R.animator.do_nothing);
                set2.setDuration(millisFromBPM(BPM, 2));
                set2.setTarget(newView);
                set2.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        try {
                            judge(nodeFragmentBindings.get(mPlant.getNode(nodeID)),
                                    true, true);
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                set2.start();
            } else {
                AnimatorSet set3 = (AnimatorSet) AnimatorInflater.loadAnimator(context,
                        R.animator.do_nothing);
                set3.setDuration(timing_miss);
                set3.setTarget(newView);
                set3.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        flag_judge = fragmentStepBindings.get(nodeID).contains(origin);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        try {
                            if (flag_judge) {
                                judge(nodeFragmentBindings.get(mPlant.getNode(nodeID)), false,
                                        false, origin);
                            }
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        if (!flag_signal) {
                            Log.d(RhythmPlantActivity.class.getSimpleName(), "fxLayer size: " + fxLayer.getChildCount());
                            fxLayer.removeView(newView);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                set3.start();
            }
            set.start();

            super.onPostExecute(objects);
        }
    }

    private class PulseTask extends AsyncTask<RhythmTrack.Step, Void, RhythmTrack.Step[]> {

        ArrayList<FlowerFragment> fragments;
        @Override
        protected RhythmTrack.Step[] doInBackground(RhythmTrack.Step... params) {
            fragments = new ArrayList<>();
            for (int i = 0; i < mPlant.getSize(); i++) {
                fragments.add(nodeFragmentBindings.get(mPlant.getNode(i)));
            }
            return new RhythmTrack.Step[0];
        }

        @Override
        protected void onPostExecute(RhythmTrack.Step[] steps) {
            for (FlowerFragment fragment : fragments) {
                fragment.pulse(FlowerFragment.PULSE, false);
            }
            super.onPostExecute(steps);
        }
    }

    private class SendSignalTask extends AsyncTask<RhythmTrack.Step, Void, RhythmTrack.Step[]> {
        RhythmTrack.Step step;
        int nodeID, nodeLevel, cooldown;
        @Override
        protected RhythmTrack.Step[] doInBackground(RhythmTrack.Step... params) {
            step = params[0];
            for (Integer num : step.getNotes()) {
                int temp = num - 1;
                if (step.getRandomize()) {
                    randomizeBindings();
                }
//                Log.d(RhythmPlantActivity.class.getSimpleName(), availableNodeList().size() + "");
                if (availableNodeList().size() == 0) {
                    terminateActivity();
                    finishActivity(SONG_FAIL_RESULT);
                }
                nodeID = availableNodeList().get(currentBindings.get(temp));
                nodeLevel = mPlant.getNode(nodeID).getLevel();
                cooldown = (int) (signalSendCooldown / signalSendCooldownWeights[nodeLevel - 1]);
                if (step.getTime() - nodeTimes[nodeID] < cooldown) {
                    int ref = nodeID;
                    for (int i = 0; i < availableNodeList().size(); i++) {
                        if (ref != i && step.getTime() - nodeTimes[i] < cooldown) {
                            nodeID = i;
                        } else {
                            break;
                        }
                    }
                    if (step.getTime() - nodeTimes[nodeID] < cooldown) {
                        notesEaten++;
                        continue;
                    }
                }
                FlowerFragment flower = nodeFragmentBindings
                        .get(mPlant.getNode(nodeID));
                if (fragmentStepBindings.containsKey(flower.getFlowerID())) {
//                    Log.d(RhythmPlantActivity.class.getSimpleName(), "fragmentStepBindings - ID: "
//                        + flower.getFlowerID() + " Size: "
//                            + fragmentStepBindings.get(flower.getFlowerID()).size());
                    fragmentStepBindings.get(flower.getFlowerID()).add(step);
                } else {
                    ArrayList<RhythmTrack.Step> newList =
                            new ArrayList<>();
                    newList.add(step);
//                    Log.d(RhythmPlantActivity.class.getSimpleName(), "fragmentStepBindings - ID: "
//                            + flower.getFlowerID() + " Size: 1");
                    fragmentStepBindings.put(flower.getFlowerID(), newList);
                }
                nodeTimes[nodeID] = step.getTime();
                generateSignal(nodeID, step.getBpm(), step);
            }

            return new RhythmTrack.Step[0];
        }

        @Override
        protected void onPostExecute(RhythmTrack.Step[] steps) {
            super.onPostExecute(steps);
        }
    }

    private class JudgeTask extends AsyncTask<Object, Void, Boolean> {
        FlowerFragment flower;
        Boolean pressed, auto;
        int resultCode;
        RhythmTrack.Step refStep;
        @Override
        protected Boolean doInBackground(Object... params) {
            try {
                resultCode = 0;
                flower = (FlowerFragment) params[0];
                pressed = (Boolean) params[1];
                auto = (Boolean) params[2];
                refStep = null;
                if (params.length > 3) {
                    refStep = (RhythmTrack.Step) params[3];
                }
                if (!fragmentStepBindings.containsKey(flower.getFlowerID()) ||
                        fragmentStepBindings.get(flower.getFlowerID()).size() == 0) {
                    return false;
                }
                if (!pressed && !AUTO_ASSIST) {
                    fragmentStepBindings.get(flower.getFlowerID()).remove(refStep);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            distributeDamage(flower.getFlowerID(), difficultyJudgeWeights
                                    .get(mTrack.getDifficulty()).get(SONG_MISS_INDEX));
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            incrementCombo(FlowerFragment.WITHER_SPARK);
                        }
                    });
                    return true;
                }
                ArrayList<RhythmTrack.Step> steps = fragmentStepBindings.get(flower.getFlowerID());
                long minDiff = Long.MAX_VALUE;
                RhythmTrack.Step minStep = steps.get(0);
                long currentTime = mPlayer.getCurrentPosition()
                        + (millisFromBPM(mTrack.getStepData().get(0).getBpm(),
                        1.95 * mTrack.getUpbeats()));
                for (RhythmTrack.Step step : steps) {
                    long diff = Math.abs((long) step.getTime() - currentTime);
                    if (minDiff > diff) {
                        minDiff = diff;
                        minStep = step;
                    }
                }
                fragmentStepBindings.get(flower.getFlowerID()).remove(minStep);
//                Log.d("test", "ID: " + flower.getFlowerID() + ", minDiff: " + minDiff
//                + ", minStepTime: " + minStep.getTime());
                if (minDiff <= BLOOM_TIMING_WINDOW || AUTO_ASSIST) {
                    flower.pulse(FlowerFragment.GROW_HIT, true);
                    flower.changeHealth(difficultyJudgeWeights
                            .get(mTrack.getDifficulty()).get(SONG_BLOOM_INDEX));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new SparkTask().execute(flower.getFlowerID(), FlowerFragment.BLOOM_SPARK);
                            incrementCombo(FlowerFragment.BLOOM_SPARK);
                        }
                    });
                    return true;
                } else if (minDiff <= GROW_TIMING_WINDOW) {
                    flower.pulse(FlowerFragment.GROW_HIT, true);
                    flower.changeHealth(difficultyJudgeWeights
                            .get(mTrack.getDifficulty()).get(SONG_GROW_INDEX));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new SparkTask().execute(flower.getFlowerID(), FlowerFragment.NORMAL_SPARK);
                            incrementCombo(FlowerFragment.NORMAL_SPARK);
                        }
                    });
                    return true;
                } else if (minDiff <= MISS_TIMING_WINDOW) {
//                    Log.d("RhythmPlantActivity", flower.getFlowerID() + "");
                    boolean test = fragmentStepBindings.get(flower.getFlowerID()).remove(minStep);
//                    Log.d("test", test + ""
//                    + " ID: " + flower.getFlowerID());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            incrementCombo(FlowerFragment.PULSE);
                            distributeDamage(flower.getFlowerID(),
                                    difficultyJudgeWeights
                                            .get(mTrack.getDifficulty()).get(SONG_NEAR_MISS_INDEX));
                        }
                    });
                    return true;
                }
            } catch (IllegalStateException e) {
                return false;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        setContentView(R.layout.activity_rhythm_plant);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorLightBlack));
        }

        if (getIntent() != null && getIntent().hasExtra(RHYTHM_SONGFILE)) {
            filename = getIntent().getStringExtra(RHYTHM_SONGFILE);
        }

        sparkGraphic = ContextCompat.getDrawable(this, R.drawable.florabeats_rhythm_spark_small);
        signalGraphic = ContextCompat.getDrawable(this, R.drawable.florabeats_rhythm_signal_2_small);

        tier1 = (LinearLayout) findViewById(R.id.ll_flower_tier_1);
        tier2 = (LinearLayout) findViewById(R.id.ll_flower_tier_2);
        tier3 = (LinearLayout) findViewById(R.id.ll_flower_tier_3);
        tier4 = (LinearLayout) findViewById(R.id.ll_flower_tier_4);
        comboLayer = (LinearLayout) findViewById(R.id.ll_combo);

        layoutArea = (ConstraintLayout) findViewById(R.id.cl_rhythm_plant);
        nodeLayoutBindings = new HashMap<>();
        nodeLineBindings = new HashMap<>();
        nodeFragmentBindings = new HashMap<>();
        fragmentStepBindings = new HashMap<>();
        mTimer = new Timer();
        mSFXTimerA = new Timer();
        mSFXTimerB = new Timer();
        mSFXTimerC = new Timer();
        sfxTimers = new ArrayList<>();
        sfxTimers.add(mSFXTimerA);
        sfxTimers.add(mSFXTimerB);
        sfxTimers.add(mSFXTimerC);
        sfxIndex = bloomCount = growCount = missCount = 0;

        resumeButton = (Button) findViewById(R.id.button_resume);
        quitButton = (Button) findViewById(R.id.button_quit);

        vineLayer = (FrameLayout) findViewById(R.id.fl_flower_vines);
        fxLayer = (FrameLayout) findViewById(R.id.fl_flower_fx);
        fxLayer2 = (FrameLayout) findViewById(R.id.fl_flower_fx2);
        pauseLayer = (FrameLayout) findViewById(R.id.fl_flower_pause);

        mLoadingImage = (ImageView) findViewById(R.id.iv_rhythm_loading);
        mLoadingText = (TextView) findViewById(R.id.tv_rhythm_loading);

        if (savedInstanceState == null) {
            mTrackTask.execute();
        }
        mReadyMessage = (TextView) findViewById(R.id.tv_ready_message);
        mCombo = (TextView) findViewById(R.id.tv_combo_count);
        mComboLabel = (TextView) findViewById(R.id.tv_combo_label);
        mScoreLabel = (TextView) findViewById(R.id.tv_rhythm_score);
        comboActivated = false;
        maxCombo = combo = 0;
        score = 0;
        notesEaten = 0;

        mDifficultyIcon = (ImageView) findViewById(R.id.iv_difficulty_icon);
        mDifficultyLabel = (TextView) findViewById(R.id.tv_difficulty_songname);
        difficultyStars = (LinearLayout) findViewById(R.id.ll_difficulty_stars);

        tier1.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
        tier2.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
        tier3.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
        tier4.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);

        difficultyJudgeWeights = new HashMap<>();
        ArrayList<Integer> easy_weights = new ArrayList<>();
        easy_weights.add(-10);
        easy_weights.add(-5);
        easy_weights.add(6);
        easy_weights.add(8);
        ArrayList<Integer> medium_weights = new ArrayList<>();
        medium_weights.add(-15);
        medium_weights.add(-7);
        medium_weights.add(4);
        medium_weights.add(6);
        ArrayList<Integer> hard_weights = new ArrayList<>();
        hard_weights.add(-20);
        hard_weights.add(-10);
        hard_weights.add(3);
        hard_weights.add(4);
        difficultyJudgeWeights.put("Sprout", easy_weights);
        difficultyJudgeWeights.put("Flower", medium_weights);
        difficultyJudgeWeights.put("Tree", hard_weights);

        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resume();
            }
        });

        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivity(SONG_CANCEL_RESULT);
            }
        });
    }

    private void addLine(int x0, int y0, int x1, int y1, int id0, int id1) {
//        Log.d("RhythmPlantActivity", x0 + " " + y0 + " " + x1 + " " + y1 + " " + id0 + " "
//                + id1);
        LineView line = new LineView(this, x0, y0, x1, y1, id0, id1, R.color.colorVine1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            line.setElevation(1.0f);
        }
        vineLayer.addView(line);
        nodeLineBindings.put(mPlant.getNode(id0), line);
    }

    private void addViewToTier(int level, View view) {
        switch (level) {
            case 1:
                tier1.addView(view);
                break;
            case 2:
                tier2.addView(view);
                break;
            case 3:
                tier3.addView(view);
                break;
            case 4:
                tier4.addView(view);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void bloom(int nodeID) {
        RhythmPlant.PlantNode node = mPlant.getNode(nodeID);
        for (RhythmPlant.PlantNode child : node.getChildren()) {
            nodeFragmentBindings.get(child).setFlowerVisibility(true);
            toggleFlowerVisibility(child.getID());
        }
    }

    public void bloomSelf(int nodeID) {
        RhythmPlant.PlantNode node = mPlant.getNode(nodeID);
        nodeFragmentBindings.get(node).setFlowerVisibility(true);
        toggleFlowerVisibility(node.getID());
    }

    public void toggleFlowerVisibility(int nodeID) {
        RhythmPlant.PlantNode node = mPlant.getNode(nodeID);
        if (nodeFragmentBindings.get(node).flowerIsVisible()) {
            nodeLineBindings.get(node).setVisibility(View.VISIBLE);
            nodeLayoutBindings.get(node).setVisibility(View.VISIBLE);
        } else {
            nodeLineBindings.get(node).setVisibility(View.INVISIBLE);
            nodeLayoutBindings.get(node).setVisibility(View.INVISIBLE);
        }
    }

    public void distributeDamage(int nodeID, int damage) {
        if (damage > 0) {
            throw new IllegalArgumentException("Damage cannot be positive!");
        } else if (damage == 0) {
            damage -= 1;
        }
        RhythmPlant.PlantNode node = mPlant.getNode(nodeID);
        int activeNeighborCount = 0;
        for (RhythmPlant.PlantNode child : node.getChildren()) {
            if (nodeFragmentBindings.get(child).flowerIsVisible()) {
                activeNeighborCount++;
            }
        }
        // Log.d(RhythmPlantActivity.class.getSimpleName(), "Node " + nodeID + " has "
        // + activeNeighborCount + " active children");
        if (activeNeighborCount == 0) {
            nodeFragmentBindings.get(node).changeHealth(damage);
            nodeFragmentBindings.get(node).pulseSelectedFlower(
                    nodeFragmentBindings.get(node).getHealthBar(), FlowerFragment.DAMAGE_HIT, false);
        } else {
            for (RhythmPlant.PlantNode child : node.getChildren()) {
                distributeDamage(child.getID(), damage / activeNeighborCount);
            }
        }
    }

    public void generateSpark(int nodeID, int mode) {
        new SparkTask().execute(nodeID, mode);
    }

    public void generateSignal(final int nodeID, int BPM, final RhythmTrack.Step origin) {
        new SignalTask().execute(nodeID, BPM, origin);
    }

    public static long millisFromBPM(double BPM) {
        return (long)(60.0 / BPM * 1000);
    }
    public static long millisFromBPM(double BPM, double times) {
        return (long)(60.0 * times / BPM * 1000);
    }

    public SparseIntArray availableNodeList() {
        int index = 0;
        SparseIntArray list = new SparseIntArray();
        for (int i = 0; i < mPlant.getSize(); i++) {
            RhythmPlant.PlantNode node = mPlant.getNode(i);
            if (nodeFragmentBindings.get(node).flowerIsVisible()) {
                list.put(index, i);
                index++;
            }
        }
        return list;
    }

    public SparseIntArray randomizedBindings(SparseIntArray nodeList) {
        int size = nodeList.size();
        ArrayList<Integer> ints = new ArrayList<>();
        ArrayList<Integer> scrambled = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ints.add(i);
            scrambled.add(i);
        }
        Collections.shuffle(scrambled);
        SparseIntArray bindings = new SparseIntArray();
        for (int i = 0; i < size; i++) {
            bindings.put(ints.get(i), scrambled.get(i));
        }
        return bindings;
    }

    public void deployRhythmTrack() {
        Log.d("RhythmPlantActivity", "STARTED DEBUGGING!");
//        if (TitleActivity.canWriteToTrace) {
//            Debug.startMethodTracing("plant_render");
//        }
        signalSendCooldown = mTrack.getStepData().get(0).getBpm();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final FrameLayout loadingScreen = (FrameLayout)
                        RhythmPlantActivity.this.findViewById(R.id.fl_flower_loading);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(RhythmPlantActivity.this,
                                R.animator.simple_fade);
                        set.setTarget(loadingScreen);
                        set.start();
                        set.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                loadingScreen.removeView(mLoadingImage);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                loadingScreen.removeView(mLoadingImage);
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                    }
                });
            }
        }, LOADING_OFFSET_TIME);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < mPlant.getSize(); i++) {
                            if (mPlant.getNode(i).getLevel() <= 2) {
                                bloomSelf(i);
                            }
                        }
                        currentBindings = randomizedBindings(availableNodeList());
                        startFlag = true;
                    }
                });
            }
        }, LOADING_OFFSET_TIME + BLOOM_IN_OFFSET_TIME);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(
                                RhythmPlantActivity.this, R.animator.text_expandout);
                        set.setTarget(mReadyMessage);
                        set.start();
                    }
                });
            }
        }, LOADING_OFFSET_TIME + READY_SIGNAL_OFFSET_TIME);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mPlayer.setLooping(false);
                mPlayer.start();
            }
        }, LOADING_OFFSET_TIME +
                millisFromBPM(mTrack.getStepData().get(0).getBpm(), mTrack.getUpbeats() * 2));
        // Log.d("test", mTrack.getUpbeats() + "");
        for (final RhythmTrack.Step step : mTrack.getStepData()) {
            if (step.getPulse()) {
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        new PulseTask().execute();
                    }
                }, Math.round(LOADING_OFFSET_TIME + step.getTime()
                        + millisFromBPM(step.getBpm(), 0.25)));
            }
            if (step.getNotes().size() > 0) {
                if (AUTO_ASSIST) {
                    sfxTimers.get(sfxIndex).schedule(new TimerTask() {
                        @Override
                        public void run() {
//                            new HitSoundTask().execute();
                        }
                    }, Math.round(LOADING_OFFSET_TIME
                            + step.getStartTime()) + millisFromBPM(step.getBpm(), 2.25));
                    sfxIndex = (sfxIndex + 1) % sfxTimers.size();
                }
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // Log.d("test", "SendSignal executed for time " + step.getTime());
                        new SendSignalTask().execute(step);
                    }
                }, Math.round(LOADING_OFFSET_TIME + step.getStartTime()) + millisFromBPM(step.getBpm(), 0.25));
            }
        }
        RhythmTrack.Step finalStep = mTrack.getStepData().get(mTrack.getStepData().size() - 1);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                terminateActivity();
                finishActivity(SONG_CLEAR_RESULT);
            }
        }, (long) finalStep.getTime() + millisFromBPM(finalStep.getBpm(), 4)
        + millisFromBPM(mTrack.getStepData().get(0).getBpm(), mTrack.getUpbeats() * 2));
    }

    public void deployRhythmTrack(double timerOffset, final double playerOffset) {
        savedTimerOffset = timerOffset;
        signalSendCooldown = mTrack.getStepData().get(0).getBpm();
        String audioFile = mTrack.getAudioname();
        long loadOffsetAndUpbeats = PAUSE_LOAD_OFFSET_TIME
                + millisFromBPM(mTrack.getStepData().get(0).getBpm(), 0);
        int audioRes = getResources().getIdentifier(audioFile, "raw", RhythmPlantActivity.this
                .getPackageName());
        mPlayer = MediaPlayer.create(this, audioRes);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mPlayer.setLooping(false);
                mPlayer.seekTo((int) playerOffset);
                mPlayer.start();
                startFadeIn();
            }
        }, loadOffsetAndUpbeats +
                millisFromBPM(mTrack.getStepData().get(0).getBpm(), mTrack.getUpbeats()));
//        Log.d("test", mTrack.getUpbeats() + "");
        for (final RhythmTrack.Step step : mTrack.getStepData()) {
            if (step.getPulse()) {
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        new PulseTask().execute();
                    }
                }, Math.round(loadOffsetAndUpbeats + step.getTime() - timerOffset)
                        + millisFromBPM(step.getBpm(), 0.25));
            }
            if (step.getNotes().size() > 0) {
//                Log.d("test", "" + step.getStartTime() + " "
//                + timerOffset + " " + PAUSE_LOAD_OFFSET_TIME);
                if (AUTO_ASSIST) {
                    sfxTimers.get(sfxIndex).schedule(new TimerTask() {
                        @Override
                        public void run() {
//                            new HitSoundTask().execute();
                        }
                    }, Math.round(loadOffsetAndUpbeats + step.getStartTime() - timerOffset)
                            + millisFromBPM(step.getBpm(), 2.25));
                    sfxIndex = (sfxIndex + 1) % sfxTimers.size();
                }
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
//                        Log.d("test", "SendSignal executed for time " + step.getTime());
                        new SendSignalTask().execute(step);
                    }
                }, Math.round(loadOffsetAndUpbeats + step.getStartTime() - timerOffset)
                        + millisFromBPM(step.getBpm(), 0.25));
            }
        }
        RhythmTrack.Step finalStep = mTrack.getStepData().get(mTrack.getStepData().size() - 1);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                terminateActivity();
                finishActivity(SONG_CLEAR_RESULT);
            }
        }, loadOffsetAndUpbeats + (long) (finalStep.getTime() - timerOffset)
                + millisFromBPM(mTrack.getStepData().get(0).getBpm(),
                mTrack.getUpbeats() * 2));
    }

    public boolean judge(final FlowerFragment flower, boolean pressed, boolean auto) {
        try {
            return new JudgeTask().execute(flower, pressed, auto).get();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.e("RhythmPlantActivity", sw.toString());
            return false;
        }
    }

    public boolean judge(final FlowerFragment flower, boolean pressed, boolean auto,
                         RhythmTrack.Step step) {
        try {
            return new JudgeTask().execute(flower, pressed, auto, step).get();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.e("RhythmPlantActivity", sw.toString());
            return false;
        }
    }

    public void randomizeBindings() {
//        Log.d("test", "randomizeBindings called");
        currentBindings = randomizedBindings(availableNodeList());
//        for (int i = 0; i < currentBindings.size(); i++) {
//            Log.d("test", "Index " + i + " yields " + currentBindings.get(i));
//        }
    }

    public void terminateActivity() {
//        Log.d("test", "Terminated player");
        mTimer.cancel();
        mPlayer.release();
        for (Timer timer : sfxTimers) {
            timer.cancel();
        }
        for (FlowerFragment fragment : nodeFragmentBindings.values()) {
            fragment.getTimer().cancel();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (startFlag) {
            pause();
        }
    }

    @Override
    public void finishActivity(int requestCode) {
        Log.d("RhythmPlantActivity", "STOPPED DEBUGGING!");
//        if (TitleActivity.canWriteToTrace) {
//            Debug.stopMethodTracing();
//        }
        if (requestCode != SONG_CLEAR_RESULT && requestCode != SONG_FAIL_RESULT) {
            setResult(requestCode);
            super.finishActivity(requestCode);
            finish();
        } else {
            finishFlag = true;
            Intent intent = new Intent(this, RhythmResultsActivity.class);
            intent.putExtra(RhythmResultsActivity.SCORE_EXTRA, mScoreLabel.getText());
            intent.putExtra(RhythmResultsActivity.BLOOM_COUNT_EXTRA, bloomCount);
            intent.putExtra(RhythmResultsActivity.GROW_COUNT_EXTRA, growCount);
            intent.putExtra(RhythmResultsActivity.MISS_COUNT_EXTRA, missCount);
            intent.putExtra(RhythmResultsActivity.MAX_COMBO_EXTRA, maxCombo);
            intent.putExtra(RhythmResultsActivity.SUCCESS_EXTRA, (requestCode == SONG_CLEAR_RESULT));
            startActivity(intent);
            finish();
        }
    }

    public void onRestart() {
        super.onRestart();
    }

    public void incrementCombo(int mode) {
        AnimatorSet set, set2;
        noteProgress++;
        switch (mode) {
            case FlowerFragment.BLOOM_SPARK:
                combo++;
                score++;
                bloomCount++;
                setScore(score);
                mCombo.setText(String.valueOf(combo));
                if (!comboActivated && combo >= 10) {
                    mCombo.setVisibility(View.VISIBLE);
                    comboLayer.setVisibility(View.VISIBLE);
                    mComboLabel.setVisibility(View.VISIBLE);
                    comboActivated = true;
                }
                mComboLabel.setText(getResources().getString(R.string.bloom_combo));
                mComboLabel.setTextColor(ContextCompat.getColor(this, R.color.colorSparkBloom));
                if (!comboAnimating) {
                    set = (AnimatorSet) AnimatorInflater.loadAnimator(
                            RhythmPlantActivity.this, R.animator.text_pulse);
                    set.setTarget(mCombo);
                    set.addListener(mComboListener);
                    set2 = (AnimatorSet) AnimatorInflater.loadAnimator(
                            RhythmPlantActivity.this, R.animator.text_pulse_alternate);
                    set2.setTarget(mComboLabel);
                    set2.start();
                    set.start();
                    comboAnimating = true;
                }
                break;
            case FlowerFragment.NORMAL_SPARK:
                combo++;
                growCount++;
                score += 0.5;
                setScore(score);
                mCombo.setText(String.valueOf(combo));
                mComboLabel.setText(getResources().getString(R.string.grow_combo));
                mComboLabel.setTextColor(ContextCompat.getColor(this, R.color.colorSparkGrow));
                if (!comboActivated && combo >= 10) {
                    mCombo.setVisibility(View.VISIBLE);
                    comboLayer.setVisibility(View.VISIBLE);
                    mComboLabel.setVisibility(View.VISIBLE);
                    comboActivated = true;
                };
                if (!comboAnimating) {
                    set = (AnimatorSet) AnimatorInflater.loadAnimator(
                            RhythmPlantActivity.this, R.animator.text_pulse);
                    set.setTarget(mCombo);
                    set.addListener(mComboListener);
                    set2 = (AnimatorSet) AnimatorInflater.loadAnimator(
                            RhythmPlantActivity.this, R.animator.text_pulse);
                    set2.setTarget(mComboLabel);
                    set2.start();
                    set.start();
                    comboAnimating = true;
                }
                break;
            case FlowerFragment.PULSE:
                combo = 0;
                missCount++;
                score += 0.2;
                setScore(score);
                mComboLabel.setTextColor(ContextCompat.getColor(this, R.color.colorWhiteCombo));
                if (comboActivated) {
                    mCombo.setVisibility(View.INVISIBLE);
                    comboLayer.setVisibility(View.INVISIBLE);
                    mComboLabel.setVisibility(View.INVISIBLE);
                    comboActivated = false;
                }
                break;
            case FlowerFragment.WITHER_SPARK:
                combo = 0;
                missCount++;
                mComboLabel.setTextColor(ContextCompat.getColor(this, R.color.colorWhiteCombo));
                if (comboActivated) {
                    mCombo.setVisibility(View.INVISIBLE);
                    comboLayer.setVisibility(View.INVISIBLE);
                    mComboLabel.setVisibility(View.INVISIBLE);
                    comboActivated = false;
                }
                break;
        }
        if (combo > maxCombo) {
            maxCombo = combo;
        }
    }

    public String scoreString(long score) {
        long init = score;
        if (score >= 1000000) {
            return String.valueOf(score);
        } else {
            String result = "";
            while (score < 1000000) {
                result += "0";
                score *= 10;
                if (score == 0) {
                    score = 10;
                }
            }
            return result + String.valueOf(init);
        }
    }

    public void setScore(double score) {
        double prescore = Math.min(score, totalNotes - notesEaten)
                / (double) (totalNotes - notesEaten) * 1000000.0;
        mScoreLabel.setText(scoreString(Math.round(prescore)));
    }

    public void provideSongInfo() {
        String songDifficulty = mTrack.getDifficulty();
        int songDifficultyLevel = mTrack.getDifficultyLevel();
        String songTitle = mTrack.getSongName();

        mDifficultyLabel.setText(songTitle);
        switch (songDifficulty) {
            case "Sprout":
                mDifficultyIcon.setImageDrawable(
                        ContextCompat.getDrawable(this, R.drawable.florabeats_difficulty_sprout));
                break;
            case "Flower":
                mDifficultyIcon.setImageDrawable(
                        ContextCompat.getDrawable(this, R.drawable.florabeats_difficulty_flower));
                break;
            case "Tree":
                mDifficultyIcon.setImageDrawable(
                        ContextCompat.getDrawable(this, R.drawable.florabeats_difficulty_tree));
                break;
            default:
                throw new IllegalStateException("Invalid difficulty specified on song: "
                + songDifficulty);
        }
        for (int i = 0; i < songDifficultyLevel; i++) {
            ImageView star = new ImageView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                            getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                            getResources().getDisplayMetrics()));
            params.setMargins((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                    getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                            getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                            getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                            getResources().getDisplayMetrics()));
            star.setLayoutParams(params);
            star.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.florabeats_difficulty_star));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                star.setImageTintList(ContextCompat.getColorStateList(this,
                        R.color.colorWhite));
            }
            difficultyStars.addView(star);
        }
    }

    @Override
    protected void onResume() {
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        super.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            AnimatorSet anim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.text_pulse);
            anim.setTarget(mLoadingText);
            anim.setDuration(600);
            anim.setStartDelay(500);
            anim.start();
        } else {
            if (!finishFlag) {
                pause();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        pause();
        outState.putDouble(SAVED_SCORE, savedScore);
        outState.putInt(SAVED_EATEN, savedEaten);
        outState.putInt(SAVED_COMBO, savedCombo);
        outState.putIntArray(SAVED_NODE_STATUS, currentNodeStatus);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        pauseFlag = true;
        unpauseFlag = false;
        pauseLayer.setVisibility(View.VISIBLE);
        score = savedInstanceState.getDouble(SAVED_SCORE);
        notesEaten = savedInstanceState.getInt(SAVED_EATEN);
        combo = savedInstanceState.getInt(SAVED_SCORE);
        currentNodeStatus = savedInstanceState.getIntArray(SAVED_NODE_STATUS);
    }

    private void pause() {
        savedScore = score;
        savedEaten = notesEaten;
        savedCombo = combo;
        pauseFlag = true;
        unpauseFlag = false;
        pauseLayer.setVisibility(View.VISIBLE);
        for (int i = 0; i < mPlant.getSize(); i++) {
            if (!nodeFragmentBindings.get(mPlant.getNode(i))
                    .flowerIsVisible()) {
                currentNodeStatus[i] = -1;
            } else {
                currentNodeStatus[i] = nodeFragmentBindings.get(mPlant.getNode(i))
                        .getHealth();
            }
        }
        terminateActivity();
    }

    private void resume() {
        mTimer = new Timer();
        for (int i = 0; i < sfxTimers.size(); i++) {
            sfxTimers.set(i, new Timer());
        }
        hasResumed = true;
        for (FlowerFragment fragment : nodeFragmentBindings.values()) {
            fragment.initTimer();
        }
        if (unpauseFlag) {
            return;
        }
        unpauseFlag = false;
        int stepCount = savedCombo + savedEaten;
        notesEaten = 0;
        double playerOffset = 0.0;
        double timerOffset = 0.0;
        score = savedScore;
        mTrack = new RhythmTrack(getIntent().getStringExtra(RHYTHM_SONGFILE), this);
        while (stepCount > 0) {
            RhythmTrack.Step currentStep = mTrack.getStepData().get(0);
            stepCount -= currentStep.getNotes().size();
            if (stepCount > 0) {
                mTrack.getStepData().remove(0);
            } else {
                if (stepCount < 0) {
                    for (int i = 0; i < -1 * stepCount; i++) {
                        mTrack.getStepData().get(0).getNotes().remove(0);
                    }
                }
                playerOffset = currentStep.getTime()
                        - millisFromBPM(mTrack.getStepData().get(0).getBpm(),
                        mTrack.getUpbeats() * 3) + millisFromBPM(148, 1.5);
                timerOffset = currentStep.getTime()
                        - millisFromBPM(mTrack.getStepData().get(0).getBpm(),
                        mTrack.getUpbeats() * 2) + millisFromBPM(148, 1.5);
            }
        }
        for (int i = 0; i < mPlant.getSize(); i++) {
            nodeFragmentBindings.get(mPlant.getNode(i)).setFlowerVisibility(false);
            toggleFlowerVisibility(i);
            if (currentNodeStatus[i] != -1) {
                bloomSelf(i);
                nodeFragmentBindings.get(mPlant.getNode(i))
                        .changeHealth(currentNodeStatus[i] - RhythmPlant.STARTING_HEALTH);
            }
        }
        pauseFlag = false;
        pauseLayer.setVisibility(View.INVISIBLE);
        totalNotes = mTrack.getNoteCount();
        deployRhythmTrack(timerOffset, playerOffset);
    }

    public boolean isPaused() {
        return pauseFlag;
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
}
