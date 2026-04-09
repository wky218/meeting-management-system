package com.cms.service;

public interface SystemMessageService {
    /**
     * 发送系统消息
     * @param meetingId 会议ID
     * @param content 消息内容
     */
    void sendSystemMessage(Long meetingId, String content);

}