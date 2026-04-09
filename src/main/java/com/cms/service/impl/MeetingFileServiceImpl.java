package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.mapper.MeetingFileMapper;
import com.cms.mapper.MeetingMapper;
import com.cms.pojo.MeetingFile;
import com.cms.service.MeetingFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingFileServiceImpl implements MeetingFileService {

    private final MeetingFileMapper fileMapper;

    @Value("${meeting.file.upload-dir}")
    private String uploadDir;

    private Path getUploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
    private final MeetingMapper meetingMapper;

    @Override
    public Result<?> uploadFile(Long meetingId, MultipartFile file, Long uploaderId) {
        try {
            // 检查目录是否存在
            Path dirPath = getUploadPath();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            // 检查会议是否存在
            if (meetingMapper.selectById(meetingId) == null) {
                return Result.error("会议不存在");
            }
            // 生成文件名和保存文件
            String originalFilename = file.getOriginalFilename();
            String fileType = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + fileType;
            Path filePath = dirPath.resolve(newFileName);
            // 保存文件
            Files.copy(file.getInputStream(), filePath);
            // 保存文件信息到数据库
            MeetingFile meetingFile = new MeetingFile();
            meetingFile.setMeetingId(meetingId);
            meetingFile.setFileName(originalFilename);
            meetingFile.setFileType(fileType);
            meetingFile.setFilePath(newFileName);
            meetingFile.setFileSize(file.getSize());
            meetingFile.setUploadTime(new Date());
            meetingFile.setUploaderId(uploaderId);

            fileMapper.insert(meetingFile);

            return Result.success(meetingFile);

        } catch (IOException e) {
            log.error("文件上传失败: ", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    @Override
    public void downloadFile(Long fileId, HttpServletResponse response) {
        try {
            // 检查参数
            if (fileId == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("文件ID不能为空");
                return;
            }

            // 获取文件信息
            MeetingFile fileInfo = fileMapper.selectById(fileId);
            if (fileInfo == null) {
                log.error("文件记录不存在, fileId: {}", fileId);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在");
                return;
            }

            Path filePath = getUploadPath().resolve(fileInfo.getFilePath());
            if (!Files.exists(filePath)) {
                log.error("文件不存在: {}", filePath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在");
                return;
            }

            // 对文件名进行 URL 编码
            String encodedFileName = URLEncoder.encode(fileInfo.getFileName(), StandardCharsets.UTF_8);
            encodedFileName = encodedFileName.replace("+", "%20");

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            try (InputStream in = Files.newInputStream(filePath);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                out.flush();
            }
        } catch (IOException e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("文件下载失败");
            } catch (IOException ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }
    @Override
    public Result<?> listFiles(Long meetingId) {
        try {
            LambdaQueryWrapper<MeetingFile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MeetingFile::getMeetingId, meetingId)
                    .orderByDesc(MeetingFile::getUploadTime);
            List<MeetingFile> files = fileMapper.selectList(wrapper);
            return Result.success(files);
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return Result.error("获取文件列表失败");
        }
    }

    @Override
    @Transactional
    public Result<?> deleteFile(Long fileId) {
        try {
            // 检查参数
            if (fileId == null) {
                return Result.error("文件ID不能为空");
            }

            // 获取文件信息
            MeetingFile meetingFile = fileMapper.selectById(fileId);
            if (meetingFile == null) {
                log.warn("尝试删除不存在的文件, fileId: {}", fileId);
                return Result.error("文件不存在");
            }

            // 删除物理文件
            Path filePath = getUploadPath().resolve(meetingFile.getFilePath());
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    log.error("物理文件删除失败: {}, filePath: {}", e.getMessage(), filePath);
                    return Result.error("文件删除失败，请稍后重试");
                }
            } else {
                log.warn("物理文件不存在: {}", filePath);
            }

            // 删除数据库记录
            int rows = fileMapper.deleteById(fileId);
            if (rows > 0) {
                return Result.success("文件删除成功");
            } else {
                return Result.error("文件删除失败");
            }

        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("文件删除失败，请稍后重试");
        }
    }
}