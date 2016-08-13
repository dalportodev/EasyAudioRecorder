package com.chomptech.easyaudiorecorder;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

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
    private final int REQUEST_AUDIO_AND_STORAGE = 0;
    private boolean micPermission, extStoragePermission;
    private ArrayAdapter adapter;
    private ListView listV;
    private String renameTemp;
    private ImageView recImg;
    private Animation flashing;

    private String[] fileList = new String[999];
    private ArrayList recList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Initialize Admob */
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-9999083812241050~5402406839");

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        /* Get permissions */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO_AND_STORAGE);
        } else {
            /* If failed to obtain permissions, program will be aware of it */
            extStoragePermission = true;
            micPermission = true;
        }

        /* UI Assignments */
        //playButton = (Button)findViewById(R.id.buttonPlay);
        recButton = (Button)findViewById(R.id.buttonRecord);
        recImg = (ImageView)findViewById(R.id.imageViewRec);
        flashing = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.flashing_animation);
        listV = (ListView)findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(MainActivity.this, R.layout.simplerow, recList);
        listV.setAdapter(adapter);

        /* Set up listView click listener to select files */
        listV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mFile = Environment.getExternalStorageDirectory().getAbsolutePath() +"/EasyAudioRecorder/" + listV.getItemAtPosition(i) + ".3gp";
                /* To change play button back to stop and quit playing audio */
                if (playing) {
                    stopPlayback();
                    //playButton.setText("Play");
                    //playing = false;
                } else {
                    //playButton.setText("Play");
                    //playing = false;
                }
                startPlayback(getCurrentFocus());
            }
        });

        /* Sets file directory */
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder");
        /* If folder doesn't exists, creates it */
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

        /* Checks if any files exist, playing recording set to false */
        fileExists(false);
        if (recList.size() < 2) {
            Toast toast = Toast.makeText(getApplicationContext(), "After a recording is created, it will play when pressed in the list above.", Toast.LENGTH_LONG);
            toast.show();
        } else {

        }
    }
    @Override
    public void onBackPressed() {
        if (recording) {
            AlertDialog.Builder delConf = new AlertDialog.Builder(this)
                    .setMessage("Do you want to stop recording?");

            delConf.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    stopRecording();
                    finish();
                }
            })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .show();
        } else if (playing) {
            stopPlayback();
            finish();
        } else {
            finish();
        }
    }

    public void shareRec(View view) {

        if (!recording) {
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
                recButton.setText("    Record    ");
                playing = false;
            } else {
                recButton.setText("    Record    ");
                playing = false;
            }

            File rec = new File(mFile);
            if (rec.exists()) {
                Uri uri = Uri.fromFile(rec);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("audio/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(shareIntent, "Share recording to..."));
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Please choose a recording to send from the list above.", Toast.LENGTH_LONG);
                toast.show();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Please press stop to finish recording before sharing.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void delete(View view) {

        if (!recording) {
        /* If playing, sets mPlayer to null and changes button back to play */
           if (playing) {
               stopPlayback();
           }  else {

           }


        /* Perform deletion task */
            final File rec = new File(mFile);
            if (rec.exists()) {
                AlertDialog.Builder delConf = new AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to delete " + rec.getName().substring(0, rec.getName().length() - 4) + "?");

                delConf.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        rec.delete();
                        fileExists(false);

                    }
                })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .show();

            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Please choose a recording to delete from the list above.", Toast.LENGTH_LONG);
                toast.show();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Please press stop to finish recording before deleting.", Toast.LENGTH_LONG);
            toast.show();
        }
    }
    public void fileExists(boolean rec) {
        if (micPermission && extStoragePermission) {
            {
                /* Get list of files current in directory */
                File folder = new File(Environment.getExternalStorageDirectory() + "/EasyAudioRecorder/");
                File files[] = folder.listFiles();
                /* re initialize array elements to "" for algorithm */
                for (int a = 0; a < fileList.length; a++) {
                    fileList[a] = "";
                }
                int numRenamed = 1;
                /* Assign files in directory to indices in 2nd array corresponding with file name */
                for (int j = 0; j < files.length; j++) {
                    if (files[j].getName().length() == 25) {
                        if (files[j].getName().substring(0, 18).equals("EasyAudioRecorder_")) {
                            fileList[Integer.valueOf(files[j].getName().substring(18, 21))] = files[j].getName();
                        } else {
                            fileList[fileList.length - numRenamed] = files[j].getName();
                            numRenamed++;
                        }
                    } else {
                        fileList[fileList.length - numRenamed] = files[j].getName();
                        numRenamed++;
                    }
                }
                /* Finds first value in tracking array not holding a file name */
                int k = 1;
                while(fileList[k] != "") {
                    k++;
                }
                /* Sets up new file name based on counted files */
                if (k < 10) {
                    mFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder/EasyAudioRecorder" + "_00" + (k) + ".3gp";
                } else if (k <100) {
                    mFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder/EasyAudioRecorder" + "_0" + (k) + ".3gp";
                } else {
                    mFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder/EasyAudioRecorder" + "_" + (k) + ".3gp";
                }

                /* Updates adapter for listView */
                if (!rec) {
                    adapter.clear();
                    for (String st : fileList) {
                        if (!st.equals("")) {
                            if (st.substring(st.length() - 4, st.length()).equals(".3gp")) {
                                adapter.add(st.substring(0, st.length() - 4));
                            } else {

                            }
                        } else {

                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }
    public void renameFile(View view) {
        if (!recording) {

            if (playing) {
                stopPlayback();
            } else {

            }

            File rec = new File(mFile);

            if (rec.exists()) {
                showRenameDialog();
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Please choose a recording to rename from the list above.", Toast.LENGTH_LONG);
                toast.show();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Please press stop to end recording before renaming the file.", Toast.LENGTH_LONG);
            toast.show();
        }

    }

    public void showRenameDialog() {
        LayoutInflater layInf = LayoutInflater.from(this);
        View prompt = layInf.inflate(R.layout.inputdialog, null);
        AlertDialog.Builder alertDial = new AlertDialog.Builder(this);
        alertDial.setView(prompt);

        final EditText editText = (EditText) prompt.findViewById(R.id.edittext);

        alertDial.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        renameTemp = editText.getText().toString() + ".3gp";
                        File rec = new File(mFile);
                        File newFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EasyAudioRecorder/" + renameTemp);
                        if (!newFile.exists()) {
                            rec.renameTo(newFile);
                            fileExists(false);
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(), "Naming conflict, please choose a different file name.", Toast.LENGTH_LONG);
                            toast.show();
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        AlertDialog rename = alertDial.create();
        rename.show();
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
            if (!recording) {
                if (!playing) {
                    mPlayer = new MediaPlayer();
                    try {
                    /* This is because after fileExists(), current mFile is always one increment ahead unless set by listView click listener */
                        if (!new File(mFile).exists()) {
                            Toast toast = Toast.makeText(getApplicationContext(), "Please choose a recording to play from the list above.", Toast.LENGTH_LONG);
                            toast.show();
                        }
                        mPlayer.setDataSource(mFile);
                        mPlayer.prepare();
                        mPlayer.start();
                        playing = true;
                        recButton.setText("       Stop      ");
                        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                mPlayer.release();
                                mPlayer = null;
                                recButton.setText("    Record    ");
                                playing = false;
                            }
                        });
                    /* When audio file is finished, releases mPlayer and resets play button */
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Prepare() failed");
                    }
                } else {
                    stopPlayback();
                }
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Please press stop to finish recording before playback.", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }
    public void stopPlayback() {
        mPlayer.release();
        mPlayer = null;
        recButton.setText("    Record    ");
        playing = false;
    }

    public void recordAudio(View view) {
        if (micPermission && extStoragePermission) {
            if (playing) {
                stopPlayback();
            } else {
                if (!recording) {

                    if (playing) {
                        stopPlayback();
                    } else {

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
                    recImg.startAnimation(flashing);
                    recButton.setText("       Stop      ");
                } else {
                    stopRecording();
                }
            }
        }
    }
    public void stopRecording() {
        try {
            mRecorder.stop();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), "Stop button pressed too quickly!", Toast.LENGTH_LONG);
            toast.show();
            File recF = new File(mFile);
            recF.delete();
        } finally {
            mRecorder.release();
            mRecorder = null;
            recButton.setText("    Record    ");
            recording = false;
            recImg.clearAnimation();
            recImg.setVisibility(View.INVISIBLE);
            fileExists(false);
        }
    }
}