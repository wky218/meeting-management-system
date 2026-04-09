package com.cms.service;

import com.cms.common.Result;

public interface SignInService {
    Result<?> signIn(Long meetingId, Long userId);
    Result<?> getSignInStatus(Long meetingId, Long userId);
    Result<?> getMeetingStatistics(Long meetingId);
}