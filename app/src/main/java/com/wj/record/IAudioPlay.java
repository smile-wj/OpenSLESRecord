package com.wj.record;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public interface IAudioPlay {

    void initPlayer(String path, int sampleRate, int channels, int bitRate);

    /**
     * 启动播放
     */
    void startPlay();

    /**
     * 停止播放
     */
    void stopPlay();
}
