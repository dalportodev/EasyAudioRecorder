package com.chomptech.easyaudiorecorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{

    private static final String LOG_TAG = "EasyAudioRecorder";
    private static String mFile = null;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private boolean recording = false;
    private boolean playing = false;
    private Button recButton;
    private Button playButton;

    private GregorianCalendar cal;
    private final int REQUEST_AUDIO_AND_STORAGE = 0;
    private boolean micPermission;
    private boolean extStoragePermission;
    private ArrayList recList = new ArrayList<String>();
    private ArrayList recListUI = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-9999083812241050~5402406839");

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO_AND_STORAGE);
        } else {
            extStoragePermission = true;
            micPermission = true;
        }

        playButton = (Button)findViewById(R.id.buttonPlay);
        recButton = (Button)findViewById(R.id.buttonRecord);
        ListView listV = (ListView)findViewById(R.id.listView);
        ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.activity_main, recList);
        listV.setAdapter(adapter);

        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder");
        if (!folder.exists()) {
            try {
                folder.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        mFile = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFile += "/EasyAudioRecorder/EasyAudioRecorder.3gp";

        fileExists();
        /* set audio control buttons to control media sound level */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        /* Permissions requests external storage writing and recording audio */




    }
    public void fileExists() {
        File recFile = new File(Environment.getExternalStorageDirectory() + "/EasyAudioRecorder/EasyAudioRecorder.3gp");
        String temp = "/EasyAudioRecorder/EasyAudioRecorder.3gp";
        this.recList.add(temp);
        int i = 1;
        while(recFile.exists()) {
            recFile = new File(Environment.getExternalStorageDirectory() + "/EasyAudioRecorder/EasyAudioRecorder" + "_" + i + ".3gp");
            temp = "/EasyAudioRecorder/EasyAudioRecorder" + "_" + i + ".3gp";
            i++;
        }
        if (i > 1) {
            mFile = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFile += "/EasyAudioRecorder/ChompTechEasyAudioRecorder" + "_" + (i - 1) + ".3gp";
        }
    }
    /* called as a result of user selection of run time permissions request */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_AND_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    extStoragePermission = true;
                    micPermission = true;
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                } else {
                    extStoragePermission = false;
                    micPermission = false;
                }
            }
        }
    }

    public void startPlayback(View view) {
        if (micPermission && extStoragePermission) {
            if (!playing) {
                mPlayer = new MediaPlayer();
                try {
                    mPlayer.setDataSource(mFile);
                    mPlayer.prepare();
                    mPlayer.start();
                    playing = true;
                    playButton.setText("Stop");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Prepare() failed");
                }
            } else {
                stopPlayback();
            }
        }
    }
    public void stopPlayback() {
        mPlayer.release();
        mPlayer = null;
        playButton.setText("Play");
        playing = false;
    }

    public void recordAudio(View view) {
        if (micPermission && extStoragePermission) {
            if (!recording) {

                fileExists();

                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setOutputFile(mFile);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                try {
                    mRecorder.prepare();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "prepare() failed");
                }
                mRecorder.start();
                recording = true;
                recButton.setText("Stop");
            } else {
                stopRecording();
            }
        }
    }
    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        recButton.setText("Record");
        recording = false;
    }
}