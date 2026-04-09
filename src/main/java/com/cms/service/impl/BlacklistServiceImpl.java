package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.mapper.BlacklistMapper;
import com.cms.mapper.MeetingMapper;
import com.cms.mapper.UserMapper;
import com.cms.mapper.MeetingParticipationMapper;
import com.cms.pojo.BlackList;
import com.cms.pojo.Meeting;
import com.cms.pojo.User;
import com.cms.pojo.MeetingParticipation;
import com.cms.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistServiceImpl implements BlacklistService {

    private final BlacklistMapper blacklistMapper;
    private final MeetingMapper meetingMapper;
    private final UserMapper userMapper;
    private final MeetingParticipationMapper participationMapper;

    @Override
    @Transactional
    public boolean addToBlacklist(Long meetingId, Long userId, int duration, Long operatorId) {
        try {
            // 验证会议和用户是否存在
            Meeting meeting = meetingMapper.selectById(meetingId);
            User user = userMapper.selectById(userId);
            User operator = userMapper.selectById(operatorId);

            if (meeting == null || user == null || operator == null) {
                log.error("会议或用户不存在 - meetingId: {}, userId: {}, operatorId: {}", 
                    meetingId, userId, operatorId);
                return false;
            }

            // 验证操作者权限
            if (!isAuthorized(meeting, operatorId)) {
                log.error("操作者没有权限 - operatorId: {}", operatorId);
                return false;
            }

            // 检查是否已在黑名单中
            if (isUserInBlacklist(meetingId, userId)) {
                log.warn("用户已在黑名单中 - meetingId: {}, userId: {}", meetingId, userId);
                return false;
            }

            // 创建黑名单记录
            BlackList blackList = new BlackList();
            blackList.setMeetingId(meetingId);
            blackList.setUserId(userId);
            blackList.setDuration(duration);
            blackList.setCreatedAt(LocalDateTime.now());

            blacklistMapper.insert(blackList);
            log.info("用户已添加到黑名单 - meetingId: {}, userId: {}, duration: {}", 
                meetingId, userId, duration);
            return true;

        } catch (Exception e) {
            log.error("添加黑名单失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeFromBlacklist(Long meetingId, Long userId, Long operatorId) {
        try {
            // 验证会议和操作者权限
            Meeting meeting = meetingMapper.selectById(meetingId);
            if (meeting == null || !isAuthorized(meeting, operatorId)) {
                return false;
            }

            // 删除黑名单记录
            LambdaQueryWrapper<BlackList> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlackList::getMeetingId, meetingId)
                  .eq(BlackList::getUserId, userId);

            int result = blacklistMapper.delete(wrapper);
            log.info("用户已从黑名单移除 - meetingId: {}, userId: {}", meetingId, userId);
            return result > 0;

        } catch (Exception e) {
            log.error("移除黑名单失败", e);
            return false;
        }
    }

    @Override
    public List<BlackList> getMeetingBlacklist(Long meetingId) {
        LambdaQueryWrapper<BlackList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlackList::getMeetingId, meetingId);
        return blacklistMapper.selectList(wrapper);
    }

    @Override
    public boolean isUserInBlacklist(Long meetingId, Long userId) {
        LambdaQueryWrapper<BlackList> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlackList::getMeetingId, meetingId)
              .eq(BlackList::getUserId, userId);
        
        BlackList blackList = blacklistMapper.selectOne(wrapper);
        if (blackList == null) {
            return false;
        }

        // 检查是否是永久黑名单或者是否在有效期内
        if (blackList.getDuration() == -1) {
            return true;
        }

        LocalDateTime expiryTime = blackList.getCreatedAt()
            .plusMinutes(blackList.getDuration());
        return LocalDateTime.now().isBefore(expiryTime);
    }

    /**
     * 检查用户是否有权限操作黑名单
     * @param meeting 会议信息
     * @param operatorId 操作者ID
     * @return 是否有权限
     */
    private boolean isAuthorized(Meeting meeting, Long operatorId) {
        // 获取会议的管理者列表
        List<Map<String, Object>> managers = blacklistMapper.selectMeetingManagers(meeting.getMeetingId());
        
        // 检查操作者是否在管理者列表中
        return managers.stream()
                .anyMatch(manager -> operatorId.equals(((Number) manager.get("user_id")).longValue()));
    }
} 