package com.cms.controller;

import com.cms.common.Result;
import com.cms.dto.SystemMessageRequest;
import com.cms.service.SystemMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-message")
@RequiredArgsConstructor
public class SystemMessageController {

    private final SystemMessageService systemMessageService;

    @PostMapping("/send")
    public Result<Void> sendSystemMessage(@RequestBody SystemMessageRequest request) {
        systemMessageService.sendSystemMessage(request.getMeetingId(), request.getContent());
        return Result.success(null);
    }
}