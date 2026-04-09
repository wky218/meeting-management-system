package com.cms.dto;

import lombok.Data;

@Data
public class AudioQualityReport {
    private Long userId;
    private Long meetingId;
    private Double packetLoss;      // 丢包率
    private Integer bitrate;        // 比特率
    private Double latency;         // 延迟
    private Integer sampleRate;     // 采样率
    private Double volume;          // 音量
    private Boolean echoCancellation; // 回音消除状态
    private Boolean noiseSuppression; // 噪声抑制状态
    private String audioDeviceInfo;   // 音频设备信息
    private String timestamp;         // 时间戳
} 