package com.wj.record;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int PERMISSION_CODE = 1000;
    private boolean mRecording = false;
    private boolean playing = false;
    private AudioRecorder audioRecorder = new AudioRecorder();
    private IAudioPlay mAudioPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);

        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
        findViewById(R.id.play).setOnClickListener(this);
        findViewById(R.id.pause).setOnClickListener(this);
        applyPermission();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        applyPermission();
    }

    private void applyPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE);
        } else {
//            setupView();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                if (!mRecording) {
                    startRecord();
                    mRecording = true;
                    Log.d("recoder", "start record");
                }
                break;
            case R.id.stop:
                if (mRecording) {
                    stopRecord();
                    mRecording = false;
                }
                Log.d("recoder", "stop record");
                break;
            case R.id.play:
                playing = true;
                startPlay();
                break;
            case R.id.pause:
                if (playing) {
                    stopPlay();
                    playing = false;
                }
                stopPlay();
                break;
        }
    }


    int sampleRateInHz = 44100;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;

    public void startRecord() {
//        int channelConfig = AudioFormat.CHANNEL_IN_5POINT1;
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat) * 2;

        Log.d("recoder", "sampleRateInHz= " + sampleRateInHz + "\nchannelConfig= " + channelConfig + "\naudioFormat= " + audioFormat + "\nbufferSizeInBytes=" + bufferSizeInBytes);

        File outFile = new File(getExternalFilesDir(null), "output.pcm");
//        audioRecorder.startRecord(48000, 2, 16, outFile.getAbsolutePath());
        audioRecorder.startRecord(sampleRateInHz, 2, channelConfig, outFile.getAbsolutePath());
    }

    public void stopRecord() {
        audioRecorder.stopRecord();
    }


    public void startPlay() {
        Log.d("recoder", "startPlay");
        if (mAudioPlayer == null) {
            mAudioPlayer = new AudioTrackPlayer();
            File outFile = new File(getExternalFilesDir(null), "output.pcm");
            mAudioPlayer.initPlayer(outFile.getAbsolutePath(), 44100, channelConfig, audioFormat);
        }
        mAudioPlayer.startPlay();
    }

    private static final String NewAudioName = "/sdcard/new.wav";
    private static final String outFile = "/sdcard/record_19700101_21_11_05.pcm";

    public void stopPlay() {
        Log.d("recoder", "stopPlay");
        if (mAudioPlayer != null) {
            mAudioPlayer.stopPlay();
        }

//        File outFile = new File(getExternalFilesDir(null), "output.pcm");
        convertPcm2Wav(outFile, NewAudioName, sampleRateInHz, 6, 16);
        Log.d("recoder", "wav转换结束");

    }

    /**
     * PCM文件转WAV文件
     *
     * @param inPcmFilePath  输入PCM文件路径
     * @param outWavFilePath 输出WAV文件路径
     * @param sampleRate     采样率，例如44100
     * @param channels       声道数 单声道：1或双声道：2
     * @param bitNum         采样位数，8或16
     */
    public static void convertPcm2Wav(String inPcmFilePath, String outWavFilePath, int sampleRate,
                                      int channels, int bitNum) {

        FileInputStream in = null;
        FileOutputStream out = null;
        byte[] data = new byte[1024];

        try {
            //采样字节byte率
            long byteRate = sampleRate * channels * bitNum / 8;

            in = new FileInputStream(inPcmFilePath);
            out = new FileOutputStream(outWavFilePath);

            //PCM文件大小
            long totalAudioLen = in.getChannel().size();

            //总大小，由于不包括RIFF和WAV，所以是44 - 8 = 36，在加上PCM文件大小
            long totalDataLen = totalAudioLen + 36;

            writeWaveFileHeader(out, totalAudioLen, totalDataLen, sampleRate, channels, byteRate);

            int length = 0;
            while ((length = in.read(data)) > 0) {
                out.write(data, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 输出WAV文件
     *
     * @param out           WAV输出文件流
     * @param totalAudioLen 整个音频PCM数据大小
     * @param totalDataLen  整个数据大小
     * @param sampleRate    采样率
     * @param channels      声道数
     * @param byteRate      采样字节byte率
     * @throws IOException
     */
    private static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                            long totalDataLen, int sampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecording) {
            stopRecord();
        }
    }

//    //音频采样率 (MediaRecoder的采样率通常是8000Hz AAC的通常是44100Hz.设置采样率为44100目前为常用的采样率，官方文档表示这个值可以兼容所有的设置）
//    private static final int mSampleRateInHz = 44100;    //声道
//    private static final int mChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO; //单声道
//    //数据格式  (指定采样的数据的格式和每次采样的大小)    //指定音频量化位数 ,在AudioFormaat类中指定了以下各种可能的常量。通常我们选择ENCODING_PCM_16BIT和ENCODING_PCM_8BIT PCM代表的是脉冲编码调制，它实际上是原始音频样本。    //因此可以设置每个样本的分辨率为16位或者8位，16位将占用更多的空间和处理能力,表示的音频也更加接近真实。
//    private static final int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
//    private static final String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "audiorecordtest.pcm";
//
//    private int mBufferSizeInBytes;
//    private AudioTrack maudioTrack;
//
//    private void playPCM_STREAM() throws FileNotFoundException {
////先估算最小缓冲区大小
//        mBufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
//
//        if (maudioTrack != null) {
//            maudioTrack.stop();
//            maudioTrack.release();
//            maudioTrack = null;
//        }
////创建AudioTrack
//        maudioTrack = new AudioTrack(
//                new AudioAttributes.Builder()
//                        .setUsage(AudioAttributes.USAGE_MEDIA)
//                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                        .build(),
//                new AudioFormat.Builder()
//                        .setSampleRate(mSampleRateInHz)
//                        .setEncoding(mAudioFormat)
//                        .setChannelMask(mChannelConfig)
//                        .build(),
//                mBufferSizeInBytes,
//                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
//        );
//        maudioTrack.play();  //这个模式需要先play
//        File file = new File(getExternalFilesDir(null), "output.pcm");
//        //File file = new File(mFileName); //原始pcm文件
//        final FileInputStream fileInputStream;
//        if (file.exists()) {
//            fileInputStream = new FileInputStream(file);
//            new Thread() {
//                @Override
//                public void run() {
//                    try {
//                        byte[] buffer = new byte[mBufferSizeInBytes];
//                        while (fileInputStream.available() > 0) {
//                            int readCount = fileInputStream.read(buffer); //一次次的读取
//                            //检测错误就跳过
//                            if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
//                                continue;
//                            }
//                            if (readCount != -1 && readCount != 0) {
////可以在这个位置用play()
//                                //输出音频数据
//                                maudioTrack.write(buffer, 0, readCount); //一次次的write输出播放
//                            }
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    Log.i("TAG", "STREAM模式播放完成");
//                }
//            }.start();
//        }
//    }

}
