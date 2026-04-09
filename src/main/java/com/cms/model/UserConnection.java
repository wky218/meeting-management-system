package com.cms.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_connection")
public class UserConnection {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long meetingId;
    private String ip;           // 用户IP地址，用于网络连接追踪
    private Integer port;        // 端口号，用于网络连接追踪
    private String status;       // 连接状态：INITIATOR, CONNECTED, DISCONNECTED
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // WebRTC相关字段
    private String connectionId;  // WebRTC连接ID，用于标识特定的P2P连接
    private String sdp;          // 存储SDP（Session Description Protocol）信息
    private String candidates;   // 存储ICE候选者信息，用于NAT穿透

    // 音频控制相关字段
    private Boolean muted;           // 是否静音
    private Integer volume;          // 音量大小（0-100）
    private String audioDevice;      // 音频设备信息
}