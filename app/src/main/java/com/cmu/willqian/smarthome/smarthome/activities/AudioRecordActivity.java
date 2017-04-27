package com.cmu.willqian.smarthome.smarthome.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.cmu.willqian.smarthome.R;
import com.cmu.willqian.smarthome.smarthome.adapter.AudioAdapter;
import com.cmu.willqian.smarthome.smarthome.models.AudioFile;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AudioRecordActivity extends AppCompatActivity {

    private Button mStartBtn;
    private Button mStopBtn;
    private Button mUploadBtn;
    private File mAudioFile;
    private File mAudioPath;
    private MediaRecorder mediaRecorder;
    private String strTempFile = "radio_";// 音频文件名的前缀
    private ListView listView;
    private AudioAdapter adapter;
    public static List<AudioFile> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        list = new ArrayList<>();
        initFilePath();
        initList();
        initButton();
    }

    private void initFilePath() {
        String path;
        if (isSDCardValid()) {
            path = Environment.getExternalStorageDirectory().toString()
                    + File.separator + "recordAudio";
            System.out.println(path);
        } else {
            path = Environment.getRootDirectory().toString()
                    + File.separator + "recordAudio";
        }
        mAudioPath = new File(path);
        if (!mAudioPath.exists()) {
            mAudioPath.mkdirs();
        }
    }

    private boolean isSDCardValid() {
        if (Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            Toast.makeText(getBaseContext(), "No SD card", Toast.LENGTH_LONG).show();
        }
        return false;
    }
    private void initList() {
        listView = (ListView) findViewById(R.id.audio_listView);
        setListEmptyView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioFile audioFile = list.get(position);
                playAudio(audioFile.getAudioFile());
            }
        });
        adapter = new AudioAdapter(AudioRecordActivity.this);
        listView.setAdapter(adapter);
    }
    private void setListEmptyView() {
        View emptyView = findViewById(R.id.empty);
        listView.setEmptyView(emptyView);
    }
    //播放录音文件
    private void playAudio(File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "audio"); //文件类型
        startActivity(intent);
    }

    //点击按钮录音
    private void initButton() {
        mStartBtn = (Button) findViewById(R.id.AudioStartBtn);
        mStopBtn = (Button) findViewById(R.id.AudioStopBtn);
        //开始按钮事件监听
        mStartBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //按钮状态,false让按钮失效，
                mStartBtn.setEnabled(false);
                mStopBtn.setEnabled(true);
                mHandler.sendEmptyMessage(MSG_RECORD);
            }
        });
        //停止按钮事件监听
        mStopBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mStartBtn.setEnabled(true);
                mStopBtn.setEnabled(false);
                mHandler.sendEmptyMessage(MSG_STOP);
            }
        });
        //初始的按钮状态
        mStartBtn.setEnabled(true);
        mStopBtn.setEnabled(false);
    }
    private static final int MSG_RECORD = 0;
    private static final int MSG_STOP = 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECORD:
                    startRecord();
                    break;
                case MSG_STOP:
                    stopRecord();
                    break;
                default:
                    break;
            }
        };
    };

    //开始录音
    private void startRecord() {
        try {
            mStartBtn.setEnabled(false);
            mStopBtn.setEnabled(true);
            //实例化MediaRecorder对象
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            //设置编码格式
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            //设置音频文件的编码
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            //设置输出文件的路径
            try {
                mAudioFile = File.createTempFile(strTempFile, ".amr", mAudioPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaRecorder.setOutputFile(mAudioFile.getAbsolutePath());
            mediaRecorder.prepare();//准备
            mediaRecorder.start();//开始
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //停止录音
    private void stopRecord() {
        if (mAudioFile != null) {
            mStartBtn.setEnabled(true);
            mStopBtn.setEnabled(false);
            mediaRecorder.stop();
            if(list.isEmpty()) list.add(new AudioFile(mAudioFile, GetFileBuildTime(mAudioFile), GetFilePlayTime(mAudioFile)));
            else {
                for(AudioFile file : list) {
                    file.getAudioFile().delete();
                }
                list.removeAll(list);
                list.add(new AudioFile(mAudioFile, GetFileBuildTime(mAudioFile), GetFilePlayTime(mAudioFile)));
            }
            adapter.updateData();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
    private String GetFilePlayTime(File file){
        Date date;
        SimpleDateFormat sy1;
        String dateFormat = "error";
        try {
            sy1 = new SimpleDateFormat("HH:mm:ss");//设置为时分秒的格式
            MediaPlayer mediaPlayer;//使用媒体库获取播放时间
            mediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(file.toString()));
            //使用Date格式化播放时间mediaPlayer.getDuration()
            date = sy1.parse("00:00:00");
            date.setTime(mediaPlayer.getDuration() + date.getTime());//用消除date.getTime()时区差
            dateFormat = sy1.format(date);

            mediaPlayer.release();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateFormat;
    }
    private String GetFileBuildTime(File file) {
        Date date = new Date(file.lastModified());//最后更新的时间
        String t;
        SimpleDateFormat sy2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置年月日时分秒
        t = sy2.format(date);
        return t;
    }

}
