package com.melonfishy.florabeats;

import android.content.Intent;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SongNavigationActivity extends AppCompatActivity {

    private TextView mSongName, mSongName2;
    private Button mSprout, mFlower, mTree, mSprout2, mFlower2, mTree2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("test", "onCreate");
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        setContentView(R.layout.activity_song_navigation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorLightBlack));
        }

        mSongName = (TextView) findViewById(R.id.tv_song_name);
        mSprout = (Button) findViewById(R.id.button_sprout);
        mFlower = (Button) findViewById(R.id.button_flower);
        mTree = (Button) findViewById(R.id.button_tree);

        mSongName2 = (TextView) findViewById(R.id.tv_song_name_2);
        mSprout2 = (Button) findViewById(R.id.button_sprout_2);
        mFlower2 = (Button) findViewById(R.id.button_flower_2);
        mTree2 = (Button) findViewById(R.id.button_tree_2);

        mSprout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSong("001_morning_sprout_S.txt");
            }
        });

        mFlower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSong("001_morning_sprout_F.txt");
            }
        });

        mTree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSong("001_morning_sprout_T.txt");
                RhythmPlantActivity.AUTO_ASSIST = true;
            }
        });

        mSprout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSong("005_poliahu_S.txt");
            }
        });

        mFlower2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSong("005_poliahu_F.txt");
            }
        });

        mTree2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSong("005_poliahu_T.txt");
                RhythmPlantActivity.AUTO_ASSIST = true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void startSong(String filename) {
        Intent intent = new Intent(SongNavigationActivity.this, RhythmPlantActivity.class);
        intent.putExtra(RhythmPlantActivity.RHYTHM_SONGFILE, filename);
        startActivityForResult(intent, RhythmPlantActivity.START_SONG_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RhythmPlantActivity.SONG_FAIL_RESULT) {
            Toast.makeText(this, "GAME OVER! TRY AGAIN!", Toast.LENGTH_LONG).show();
        } else if (resultCode == RhythmPlantActivity.SONG_CANCEL_RESULT) {
            Toast.makeText(this, "SONG CANCELED", Toast.LENGTH_LONG).show();
        } else if (resultCode == RhythmPlantActivity.SONG_CLEAR_RESULT) {
            Toast.makeText(this, "SONG CLEARED!", Toast.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, TitleActivity.class);
        startActivity(intent);
    }
}
