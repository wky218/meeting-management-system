package com.cms.service;

import com.cms.common.Result;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

public interface MeetingFileService {
    Result<?> uploadFile(Long meetingId, MultipartFile file,Long uploaderId);
    void downloadFile(Long fileId, HttpServletResponse response);
    Result<?> listFiles(Long meetingId);
    Result<?> deleteFile(Long fileId);
}