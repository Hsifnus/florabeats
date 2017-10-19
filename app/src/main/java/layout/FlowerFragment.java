package layout;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.melonfishy.florabeats.R;
import com.melonfishy.florabeats.RhythmPlantActivity;
import com.melonfishy.florabeats.data.RhythmPlant;

import java.security.spec.ECField;
import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;

public class FlowerFragment extends Fragment {

    private static final int LEVEL_1_THRESHOLD = 0;
    private static final int LEVEL_2_THRESHOLD = 30;
    private static final int LEVEL_3_THRESHOLD = 50;
    private static final int LEVEL_4_THRESHOLD = 70;
    private static final int LEVEL_5_THRESHOLD = 90;

    public static final int NEUTRAL_HIT = 100;
    public static final int DAMAGE_HIT = 101;
    public static final int GROW_HIT = 102;
    public static final int PULSE = 103;

    public static final int NORMAL_SPARK = 200;
    public static final int SPROUT_SPARK = 201;
    public static final int WITHER_SPARK = 202;
    public static final int BLOOM_SPARK = 203;

    ProgressBar mProgressBar;
    int health, flowerID, nodeLevel;
    Timer mTimer;
    boolean visible = true;
    boolean isJudging = false;
    FrameLayout mFrame;

    private class PulseFlowerTask extends AsyncTask<Object, Void, Object[]> {
        ProgressBar pb;
        Integer mode;
        Boolean checkForBloom;
        @Override
        protected Object[] doInBackground(Object... params) {
            try {
                pb = (ProgressBar) params[0];
                mode = (Integer) params[1];
                checkForBloom = (Boolean) params[2];
            } catch (Exception e) {
                return new Object[0];
            }
            return new Object[0];
        }

        @Override
        protected void onPostExecute(Object[] objects) {
            try {
                if (health >= RhythmPlant.GROWTH_THRESHOLD && checkForBloom) {
                    ((RhythmPlantActivity) getActivity()).bloom(flowerID);
                }
                if (mode == PULSE) {
                    pb.setScaleX(1.1f);
                    pb.setScaleY(1.1f);
                    pb.animate().scaleX(1f).scaleY(1f).setDuration(90)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    pb.setBackgroundTintList(null);
                                    pb.getProgressDrawable().setTint(ContextCompat.getColor
                                            (getActivity(), R.color.colorHealthAccent));
                                }
                            } catch (Exception e) {
                                pb.clearAnimation();
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                } else {
                    pb.setScaleX(1.2f);
                    pb.setScaleY(1.2f);
                    pb.animate().scaleX(1f).scaleY(1f).setDuration(30)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                pb.setBackgroundTintList(null);
                                pb.getProgressDrawable().setTint(ContextCompat.getColor
                                        (getActivity(), R.color.colorHealthAccent));
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                }
                if (health >= LEVEL_5_THRESHOLD) {
                    pb.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.flora1_lv5));
                } else if (health >= LEVEL_4_THRESHOLD) {
                    pb.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.flora1_lv4));
                } else if (health >= LEVEL_3_THRESHOLD) {
                    pb.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.flora1_lv3));
                } else if (health >= LEVEL_2_THRESHOLD) {
                    pb.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.flora1_lv2));
                } else if (health >= LEVEL_1_THRESHOLD) {
                    pb.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.flora1_lv1));
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    switch (mode) {
                        case NEUTRAL_HIT:
                            pb.setBackgroundTintList(ContextCompat.getColorStateList(getActivity(),
                                    R.color.colorWhite));
                            pb.getProgressDrawable().setTint(ContextCompat.getColor(getActivity(),
                                    R.color.colorWhite));
                            break;
                        case DAMAGE_HIT:
                            pb.setBackgroundTintList(ContextCompat.getColorStateList(getActivity(),
                                    R.color.colorDamage));
                            pb.getProgressDrawable().setTint(ContextCompat.getColor(getActivity(),
                                    R.color.colorDamage));
                            break;
                        case GROW_HIT:
                            pb.setBackgroundTintList(ContextCompat.getColorStateList(getActivity(),
                                    R.color.colorGrow));
                            pb.getProgressDrawable().setTint(ContextCompat.getColor(getActivity(),
                                    R.color.colorGrow));
                            break;
                        case PULSE:
//                    pb.getBackground().setTint(ContextCompat.getColor(getActivity(),
//                                R.color.colorWhitePulse));
                            break;
                    }
                }
                super.onPostExecute(objects);
            } catch (Exception e) {
                this.cancel(true);
            }
        }
    }

    public static FlowerFragment newInstance(int id, int h, int lvl) {
        FlowerFragment f = new FlowerFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        args.putInt("health", h);
        args.putInt("level", lvl);
        f.setArguments(args);
        return f;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (getArguments() != null) {
            flowerID = getArguments().getInt("id");
            health = getArguments().getInt("health");
            nodeLevel = getArguments().getInt("level");
        }
        visible = false;
        return inflater.inflate(R.layout.fragment_flower, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        mFrame = (FrameLayout) getView().findViewById(R.id.fl_flower_layout);
        mProgressBar = (ProgressBar) getView().findViewById(R.id.pb_flower);
        mProgressBar.setMax(100);
        mProgressBar.setProgress(health);
        mTimer = new Timer();
        pulseSelectedFlower(mProgressBar, NEUTRAL_HIT, true);
        mProgressBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (((RhythmPlantActivity) getActivity()).isPaused()) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN && visible) {
                    if (RhythmPlantActivity.AUTO_ASSIST ||
                            !((RhythmPlantActivity) getActivity()).judge(FlowerFragment.this, true,
                            false)) {
                        pulse(NEUTRAL_HIT, false);
                    }
                }
                return true;
            }
        });
    }

    public void pulse(int mode, boolean bloom) {
        pulseSelectedFlower(mProgressBar, mode, bloom);
    }

    public void changeHealth(int delta) {
        if (health == 100 && delta > 0) {
            return;
        } else if (health == 0 && delta < 0) {
            return;
        }
        boolean initState = visible;
        health += delta;
        health = Math.min(100, health);
        health = Math.max(0, health);
        mProgressBar.setProgress(health);
//        Log.d("test", "Health: " + health + " ID: " + getFlowerID() + " Delta: " + delta);
        if (visible && health == 0) {
            generateSpark(WITHER_SPARK);
            ((RhythmPlantActivity) getActivity()).randomizeBindings();
        }
        visible = visible && health > 0;
        if (initState != visible) {
            ((RhythmPlantActivity) getActivity()).toggleFlowerVisibility(flowerID);
        }
    }

    public void generateSpark(int code) {
        ((RhythmPlantActivity) getActivity()).generateSpark(flowerID, code);
    }

    public void pulseSelectedFlower(final ProgressBar pb, int mode, boolean checkForBloom) {
        new PulseFlowerTask().execute(pb, mode, checkForBloom);
    }

    public void setFlowerVisibility(boolean v) {
        if (!visible && v) {
            health = RhythmPlant.STARTING_HEALTH;
            generateSpark(SPROUT_SPARK);
        }
        visible = v;
    }

    public boolean flowerIsVisible() {
        return visible;
    }

    public ProgressBar getHealthBar() {
        return mProgressBar;
    }

    public int getFlowerID() {
        return flowerID;
    }

    public Timer getTimer() {
        return mTimer;
    }

    public void setJudging(boolean b) {
        isJudging = b;
    }

    public boolean judgeStatus() {
        return isJudging;
    }

    public int getHealth() {
        return health;
    }

    public void initTimer() {
        mTimer = new Timer();
    }
}
