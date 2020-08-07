package com.wj.record;


public class AudioRecorder {

    static {
        System.loadLibrary("native-recorder");
    }

    public native void startRecord(int sampleRate, int channels, int bitRate, String pcmPath);

    public native void stopRecord();
}
