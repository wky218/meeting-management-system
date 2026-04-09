//文件管理：文件查询、上传、下载、删除
package com.cms.controller;

import com.cms.common.Result;
import com.cms.service.MeetingFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/meeting/file")
@RequiredArgsConstructor
public class MeetingFileController {

    private final MeetingFileService fileService;


    @PostMapping("/upload")
    public Result<?> uploadFile(
            @RequestParam("meetingId") Long meetingId ,
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") Long uploaderId) {
        return fileService.uploadFile(meetingId, file, uploaderId);
    }
    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable Long fileId, HttpServletResponse response) {
        fileService.downloadFile(fileId, response);
    }

    @GetMapping("/list/{meetingId}")
    public Result<?> listFiles(@PathVariable Long meetingId) {
        return fileService.listFiles(meetingId);
    }

    @DeleteMapping("/delete/{fileId}")
    public Result<?> deleteFile(@PathVariable Long fileId) {
        return fileService.deleteFile(fileId);
    }
}