package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.common.Result;
import com.cms.mapper.*;
import com.cms.pojo.Meeting;
import com.cms.pojo.Tag;
import com.cms.service.MeetingRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRecommendServiceImpl implements MeetingRecommendService {

    private final MeetingTagRelationMapper meetingTagRelationMapper;
    private final UserTagRelationMapper userTagRelationMapper;
    private final MeetingMapper meetingMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    @Override
    public Result<?> recommendPublicMeetings(Long userId, Integer limit) {
        try {
            // 1. 获取用户的标签
            List<Tag> userTags = userTagRelationMapper.findTagsByUserId(userId);
            Set<Long> userTagIds = userTags.isEmpty() ? Collections.emptySet() :
                    userTags.stream()
                            .map(Tag::getId)
                            .collect(Collectors.toSet());

            // 2. 获取所有公开会议
            List<Meeting> publicMeetings = meetingMapper.findPublicMeetings();
            if (publicMeetings.isEmpty()) {
                return Result.success(Collections.emptyList());
            }

            // 3. 计算每个会议的匹配分数
            List<Map<String, Object>> recommendedMeetings = new ArrayList<>();

            for (Meeting meeting : publicMeetings) {
                // 获取会议关联的标签
                List<Tag> meetingTags = meetingTagRelationMapper.findTagsByMeetingId(meeting.getMeetingId());

                // 如果会议没有关联标签，但有会议类型，则通过会议类型查找对应的标签
                if (meetingTags.isEmpty() && meeting.getMeetingType() != null) {
                    Tag typeTag = tagMapper.findByTagName(meeting.getMeetingType());
                    if (typeTag != null) {
                        meetingTags = Collections.singletonList(typeTag);
                    }
                }

                // 计算匹配分数
                double matchScore = calculateMatchScore(userTagIds, meetingTags);

                Map<String, Object> recommendedMeeting = new HashMap<>();
                recommendedMeeting.put("meeting", meeting);
                recommendedMeeting.put("matchScore", matchScore);
                recommendedMeetings.add(recommendedMeeting);
            }

            // 4. 按匹配分数降序排序
            recommendedMeetings.sort((a, b) ->
                    Double.compare((Double) b.get("matchScore"), (Double) a.get("matchScore")));

            return Result.success(recommendedMeetings);
        } catch (Exception e) {
            log.error("推荐会议失败", e);
            return Result.error("推荐会议失败：" + e.getMessage());
        }
    }

    private double calculateMatchScore(Set<Long> userTagIds, List<Tag> meetingTags) {
        if (userTagIds.isEmpty() || meetingTags.isEmpty()) {
            return 0.0;
        }

        Set<Long> meetingTagIds = meetingTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        // 计算Jaccard相似度
        Set<Long> intersection = new HashSet<>(userTagIds);
        intersection.retainAll(meetingTagIds);

        Set<Long> union = new HashSet<>(userTagIds);
        union.addAll(meetingTagIds);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Override
    public Result<?> getPrivateMeetings(Long userId) {
        try {
            // 获取私有会议列表
            List<Meeting> privateMeetings = meetingMapper.findUserPrivateMeetings(userId);

            // 为每个会议添加管理员信息
            for (Meeting meeting : privateMeetings) {
                try {
                    // 获取管理者信息（包含主持人和管理员）
                    List<Map<String, Object>> managers = meetingMapper.selectMeetingManagers(meeting.getMeetingId());
                    meeting.setManagers(managers);
                } catch (Exception e) {
                    log.error("获取会议管理者信息失败: meetingId={}, error={}", meeting.getMeetingId(), e.getMessage(), e);
                    meeting.setManagers(Collections.emptyList());
                }
            }

            return Result.success(privateMeetings);
        } catch (Exception e) {
            log.error("获取私有会议失败", e);
            return Result.error("获取私有会议失败：" + e.getMessage());
        }
    }
}