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
import android.widget.AdapterView;
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
    private ArrayAdapter adapter;
    private ListView listV;
    private String selected;

    private String[] fileList = new String[999];
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
        listV = (ListView)findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(MainActivity.this, R.layout.simplerow, recList);
        listV.setAdapter(adapter);

        listV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mFile = Environment.getExternalStorageDirectory().getAbsolutePath() +"/EasyAudioRecorder/" + listV.getItemAtPosition(i);
                selected = (String)listV.getItemAtPosition(i);
                /* To change play button back to stop and quit playing audio */
                if (mPlayer  != null) {
                    mPlayer.release();
                    mPlayer = null;
                    playButton.setText("Play");
                    playing = false;
                } else {
                    playButton.setText("Play");
                    playing = false;
                }
            }
        });

        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder");
        if (!folder.exists()) {
            try {
                folder.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }

        /* set audio control buttons to control media sound level */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        /* Permissions requests external storage writing and recording audio */
        for (int a = 0; a < fileList.length; a++) {
                fileList[a] = "";
        }
        fileExists(false);
    }
    public void delete(View view) {

        if (mPlayer  != null) {
            mPlayer.release();
            mPlayer = null;
            playButton.setText("Play");
            playing = false;
        } else {
            playButton.setText("Play");
            playing = false;
        }
        adapter.clear();
        File rec = new File(mFile);
        fileList[Integer.valueOf(selected.substring(18,21))] = "";
        for (String st : fileList) {
            if (!st.equals("")) {
                adapter.add(st);
            }
        }
        adapter.notifyDataSetChanged();
        rec.delete();
    }
    public void fileExists(boolean rec) {
        if (micPermission && extStoragePermission) {
            {
                if (!rec) {
                    adapter.clear();
                }
                File folder = new File(Environment.getExternalStorageDirectory() + "/EasyAudioRecorder/");
                File files[] = folder.listFiles();

                for (int j = 0; j < files.length; j++) {
                    fileList[Integer.valueOf(files[j].getName().substring(18,21))] = files[j].getName();
                }

                int k = 1;
                while(fileList[k] != "") {
                    k++;
                }
                if (k < 10) {
                    mFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder/EasyAudioRecorder" + "_00" + (k) + ".3gp";
                } else if (k <100) {
                    mFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder/EasyAudioRecorder" + "_0" + (k) + ".3gp";
                } else {
                    mFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder/EasyAudioRecorder" + "_" + (k) + ".3gp";
                }
                if (!rec) {
                    for (String st : fileList) {
                        if (!st.equals("")) {
                            adapter.add(st);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }
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

                if (mPlayer  != null) {
                    mPlayer.release();
                    mPlayer = null;
                    playButton.setText("Play");
                    playing = false;
                } else {
                    playButton.setText("Play");
                    playing = false;
                }
                fileExists(true);

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
        fileExists(false);
    }
}