package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.Result;
import com.cms.dto.MeetingCreateDTO;
import com.cms.dto.MeetingQueryDTO;
import com.cms.dto.MeetingSearchDTO;
import com.cms.dto.UserMeetingParticipationDTO;
import com.cms.enums.MeetingVisibility;
import com.cms.enums.ParticipantRole;
import com.cms.mapper.*;
import com.cms.pojo.*;
import com.cms.service.LeaveRequestService;
import com.cms.service.MeetingService;
import com.cms.service.WebRTCService;
import com.cms.vo.MeetingVO;
import com.cms.vo.ParticipantVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingMapper meetingMapper;
    private final MeetingParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final MeetingParticipantMapper meetingParticipantMapper;
    private final MeetingParticipationMapper participationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final MeetingStatisticsMapper meetingStatisticsMapper;
    @Override
    @Transactional
    public Result<?> createPublicMeeting(MeetingCreateDTO meetingDTO) {
        try {
            // 验证发起人是否存在
            if (userMapper.selectById(meetingDTO.getOrganizerId()) == null) {
                return Result.error("发起人不存在");
            }

            // 创建会议
            Meeting meeting = new Meeting();
            BeanUtils.copyProperties(meetingDTO, meeting);
            meeting.setMeetingId(null);  // 让MyBatis-Plus自动生成ID
            meeting.setStatus("待开始");
            meeting.setVisibility(MeetingVisibility.PUBLIC);
            meetingMapper.insert(meeting);

            // 添加发起人作为主持人
            MeetingParticipant organizer = new MeetingParticipant();
            organizer.setMeetingId(meeting.getMeetingId());
            organizer.setUserId(meetingDTO.getOrganizerId());
            organizer.setRole(ParticipantRole.HOST);
            participantMapper.insert(organizer);

            // 如果有密码，验证密码长度
            if (StringUtils.hasText(meetingDTO.getJoinPassword()) &&
                    (meetingDTO.getJoinPassword().length() < 4 || meetingDTO.getJoinPassword().length() > 20)) {
                return Result.error("会议密码长度必须在4-20位之间");
            }

            // 查询主持人信息
            List<ParticipantVO> participants = meetingMapper.getMeetingParticipantsList(meeting.getMeetingId());
            meeting.setParticipants(participants);

            return Result.success(meeting);
        } catch (Exception e) {
            log.error("创建公开会议失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建公开会议失败", e);
        }
    }

    @Override
    @Transactional
    public Result<?> createMeeting(MeetingCreateDTO meetingDTO) {
        try {
            // 验证发起人是否存在
            if (userMapper.selectById(meetingDTO.getOrganizerId()) == null) {
                return Result.error("发起人不存在");
            }
            // 先验证所有参与者是否存在
            for (Long userId : meetingDTO.getParticipantIds()) {
                if (userMapper.selectById(userId) == null) {
                    return Result.error("用户ID " + userId + " 不存在");
                }
            }
            // 创建会议
            Meeting meeting = new Meeting();
            BeanUtils.copyProperties(meetingDTO, meeting);
            meeting.setMeetingId(null);
            meeting.setStatus("待开始");
            meeting.setVisibility(MeetingVisibility.PRIVATE);
            meetingMapper.insert(meeting);

            // 将会议发起者身份置为HOST
            MeetingParticipant organizer = new MeetingParticipant();
            organizer.setMeetingId(meeting.getMeetingId());
            organizer.setUserId(meetingDTO.getOrganizerId());
            organizer.setRole(ParticipantRole.HOST);
            organizer.setLeaveStatus("正常");
            participantMapper.insert(organizer);

            // 添加其他参会人员
            for (Long userId : meetingDTO.getParticipantIds()) {
                // 跳过发起人，因为已经添加为主持人
                if (userId.equals(meetingDTO.getOrganizerId())) {
                    continue;
                }
                MeetingParticipant participant = new MeetingParticipant();
                participant.setMeetingId(meeting.getMeetingId());
                participant.setUserId(userId);
                participant.setRole(ParticipantRole.PARTICIPANT);
                participant.setLeaveStatus("正常");
                participantMapper.insert(participant);
            }

            // 查询参会人员列表
            List<ParticipantVO> participants = meetingMapper.getMeetingParticipantsList(meeting.getMeetingId());
            meeting.setParticipants(participants);

            return Result.success(meeting);
        } catch (Exception e) {
            log.error("创建会议失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建会议失败", e);
        }
    }
    @Override
    public Result<Page<Meeting>> listMeetings(MeetingQueryDTO queryDTO) {
        Page<Meeting> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<Meeting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Meeting::getVisibility, MeetingVisibility.PUBLIC)
                .like(StringUtils.hasText(queryDTO.getTitle()), Meeting::getTitle, queryDTO.getTitle())
                .ge(queryDTO.getStartTime() != null, Meeting::getStartTime, queryDTO.getStartTime())
                .le(queryDTO.getEndTime() != null, Meeting::getEndTime, queryDTO.getEndTime())
                .eq(StringUtils.hasText(queryDTO.getStatus()), Meeting::getStatus, queryDTO.getStatus())
                .orderByDesc(Meeting::getStartTime);

        Page<Meeting> result = meetingMapper.selectPage(page, wrapper);

        // 处理每个会议的管理者信息
        for (Meeting meeting : result.getRecords()) {
            try {
                // 获取管理者信息（包含主持人和管理员）
                List<Map<String, Object>> managers = meetingMapper.selectMeetingManagers(meeting.getMeetingId());
                meeting.setManagers(managers);  // 需要在Meeting类中添加managers字段
                meeting.setParticipants(null);  // 公开会议不需要参与者列表

            } catch (Exception e) {
                log.error("获取会议管理者信息失败: meetingId={}, error={}", meeting.getMeetingId(), e.getMessage(), e);
                meeting.setManagers(Collections.emptyList());
            }
        }

        return Result.success(result);
    }
    @Override
    public Result<Meeting> getMeetingDetail(Long id) {
        Meeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        // 根据会议可见性处理参会人员列表
        if (MeetingVisibility.PRIVATE.equals(meeting.getVisibility())) {
            try {
                List<ParticipantVO> participants = meetingMapper.getMeetingParticipantsList(meeting.getMeetingId());
                log.info("Meeting {} participants: {}", meeting.getMeetingId(), participants); // 添加日志
                meeting.setParticipants(participants != null ? participants : Collections.emptyList());
            } catch (Exception e) {
                log.error("获取会议参与者失败: meetingId={}, error={}", meeting.getMeetingId(), e.getMessage(), e);
                meeting.setParticipants(Collections.emptyList());
            }
        } else {
            meeting.setParticipants(null);
        }

        return Result.success(meeting);
    }
    @Override
    public boolean isParticipant(Long meetingId, Long userId) {
        try {
            Meeting meeting = meetingMapper.selectById(meetingId);
            if (meeting == null) {
                return false;
            }
            List<User> participants = meetingMapper.getMeetingParticipants(meetingId);
            return participants.contains(userId);
        } catch (Exception e) {
            log.error("检查会议参与者失败: {}", e.getMessage(), e);
            return false;
        }
    }
    @Override
    @Transactional
    public Result<?> cancelMeeting(Long meetingId, Long userId) {
        // 1. 查询会议并进行空值检查
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        // 2. 验证是否为会议发起者
        if (!meeting.getOrganizerId().equals(userId)) {
            return Result.error("只有会议发起者可以取消会议");
        }

        try {
            // 3. 删除相关的统计记录
            meetingStatisticsMapper.deleteByMeetingId(meetingId);

            // 4. 删除相关的请假记录
            leaveRequestMapper.deleteByMeetingId(meetingId);

            // 5. 删除会议参与者关系
            meetingParticipantMapper.deleteByMeetingId(meetingId);

            // 6. 删除会议
            meetingMapper.deleteById(meetingId);

            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除会议失败: {}", e.getMessage(), e);
            return Result.error("删除会议失败");
        }
    }
    @Override
    public Result<?> getUserMeetings(Long userId) {
        List<MeetingVO> meetings = meetingMapper.getUserMeetings(userId);

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        for (MeetingVO meeting : meetings) {
            // 直接使用 LocalDateTime，不需要转换
            LocalDateTime meetingTime = meeting.getStartTime();

            // 设置会议状态和提醒信息
            if (meetingTime.toLocalDate().equals(today)) {
                meeting.setIsToday(true);
                if (meetingTime.isBefore(now)) {
                    meeting.setStatus("进行中");
                } else {
                    Duration duration = Duration.between(now, meetingTime);
                    if (duration.toMinutes() <= 30) {
                        meeting.setIsUpcoming(true);
                        meeting.setStatus("即将开始");
                        meeting.setReminderMessage(String.format("会议《%s》将在%d分钟后开始，请做好准备",
                                meeting.getTitle(), duration.toMinutes()));
                    } else {
                        meeting.setStatus("未开始");
                        meeting.setReminderMessage(String.format("您今天%s有会议：%s",
                                meetingTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                meeting.getTitle()));
                    }
                }
            } else if (meetingTime.toLocalDate().isAfter(today)) {
                meeting.setStatus("未开始");
            } else {
                meeting.setStatus("已结束");
            }
        }

        return Result.success(meetings);
    }
    @Override
    public Result<?> startMeeting(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        if (!"待开始".equals(meeting.getStatus())) {
            return Result.error("只有未开始的会议才能开始");
        }

        meeting.setStatus("进行中");
        meetingMapper.updateById(meeting);
        return Result.success("会议已开始");
    }

    @Override
    public Result<?> getMeetingParticipants(Long meetingId) {
        if (meetingId == null) {
            return Result.error("会议ID不能为空");
        }

        // 检查会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        try {
            // 获取所有参与者信息（包括HOST、ADMIN和PARTICIPANT以及状态信息）
            List<Map<String, Object>> allParticipants = meetingMapper.selectAllMeetingParticipantsWithStatus(meetingId);
            return Result.success(allParticipants);
        } catch (Exception e) {
            log.error("获取会议参与者失败: meetingId={}, error={}", meetingId, e.getMessage(), e);
            return Result.error("获取会议参与者失败：" + e.getMessage());
        }
    }
    @Override
    public Result<?> searchPublicMeetings(MeetingSearchDTO searchDTO) {
        log.info("搜索公开会议，参数：{}", searchDTO);

        try {
            List<Meeting> meetings;

            // 如果没有搜索条件，则获取所有公开会议
            if (StringUtils.isEmpty(searchDTO.getMeetingId()) &&
                    StringUtils.isEmpty(searchDTO.getTitle()) &&
                    StringUtils.isEmpty(searchDTO.getMeetingType())) {
                meetings = meetingMapper.findPublicMeetings();
            } else {
                // 有搜索条件时，按条件搜索并过滤公开会议
                meetings = meetingMapper.searchMeetings(searchDTO)
                        .stream()
                        .filter(meeting -> MeetingVisibility.PUBLIC.equals(meeting.getVisibility()))
                        .collect(Collectors.toList());
            }

            // 转换为VO
            List<MeetingVO> meetingVOs = meetings.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            return Result.success(meetingVOs);
        } catch (Exception e) {
            log.error("搜索公开会议失败：", e);
            return Result.error("搜索公开会议失败：" + e.getMessage());
        }
    }
    @Override
    public Result<?> searchPrivateMeetings(MeetingSearchDTO searchDTO) {
        log.info("搜索私有会议，参数：{}", searchDTO);

        try {
            // 确保userId不为空
            if (searchDTO.getUserId() == null) {
                return Result.error("用户ID不能为空");
            }

            // 获取搜索结果并过滤私有会议
            List<Meeting> meetings = meetingMapper.searchMeetings(searchDTO)
                    .stream()
                    .filter(meeting -> MeetingVisibility.PRIVATE.equals(meeting.getVisibility()))
                    .filter(meeting -> {
                        // 获取会议的所有参与者信息
                        List<Map<String, Object>> participants = meetingMapper.selectAllMeetingParticipants(meeting.getMeetingId());
                        // 检查当前用户是否是该会议的参与者（包括任何角色）
                        return participants.stream()
                                .anyMatch(p -> searchDTO.getUserId().equals(p.get("user_id")));
                    })
                    .collect(Collectors.toList());

            return Result.success(meetings.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("搜索私有会议失败：", e);
            return Result.error("搜索私有会议失败：" + e.getMessage());
        }
    }
    private MeetingVO convertToVO(Meeting meeting) {
        MeetingVO vo = new MeetingVO();
        BeanUtils.copyProperties(meeting, vo);
        if (meeting.getOrganizerId() != null) {
            User organizer = userMapper.selectById(meeting.getOrganizerId());
            if (organizer != null) {
                vo.setOrganizerName(organizer.getUsername());
            }
        }
        return vo;
    }
    @Override
    @Transactional
    public Result<?> joinMeeting(Long meetingId, Long userId, String password) {
        // 检查会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        // 检查是否已经是参与者
        if (isParticipant(meetingId, userId)) {
            return Result.error("您已经是会议参与者");
        }

        // 检查会议可见性
        if (meeting.getVisibility() == MeetingVisibility.PRIVATE) {
            return Result.error("该会议为私密会议，不能主动加入");
        }

        // 检查密码
        if (StringUtils.hasText(meeting.getJoinPassword())) {
            if (!meeting.getJoinPassword().equals(password)) {
                return Result.error("会议密码错误");
            }
        }

        // 添加参与者
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeetingId(meetingId);
        participant.setUserId(userId);
        participant.setRole(ParticipantRole.PARTICIPANT);
        participantMapper.insert(participant);

        return Result.success("加入会议成功");
    }
    @Override
    public Result<?> updateMeeting(Long meetingId, String title, String description,
                                   LocalDateTime startTime, LocalDateTime endTime, String location,
                                   MeetingVisibility visibility, String meetingType)  {
        if (meetingId == null) {
            return Result.error("会议ID不能为空");
        }
        // 获取会议信息
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }
        // 修改标题
        if (StringUtils.hasText(title)) {
            meeting.setTitle(title);
        }
        // 修改描述
        if (StringUtils.hasText(description)) {
            meeting.setDescription(description);
        }
        // 修改时间
        if (startTime != null && endTime != null) {
            if (startTime.isAfter(endTime)) {
                return Result.error("开始时间不能晚于结束时间");
            }
            meeting.setStartTime(startTime);
            meeting.setEndTime(endTime);
        } else if (startTime != null) {
            if (startTime.isAfter(meeting.getEndTime())) {
                return Result.error("开始时间不能晚于结束时间");
            }
            meeting.setStartTime(startTime);
        } else if (endTime != null) {
            if (meeting.getStartTime().isAfter(endTime)) {
                return Result.error("开始时间不能晚于结束时间");
            }
            meeting.setEndTime(endTime);
        }

        // 验证地点
        if (StringUtils.hasText(location)) {
            meeting.setLocation(location);
        }
        // 修改可见性
        if (visibility != null) {
            meeting.setVisibility(visibility);
        }
        if (StringUtils.hasText(meetingType)) {
            meeting.setMeetingType(meetingType);
        }

        // 更新会议信息
        meetingMapper.updateById(meeting);
        return Result.success("会议信息修改成功");
    }

    @Override
    public Result<?> addMeetingAdmin(Long meetingId, Long userId, Long currentUserId) {
        if (meetingId == null || userId == null || currentUserId == null) {
            return Result.error("参数不能为空");
        }
        // 检查会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }
        // 检查当前操作用户是否是会议发起者
        if (!meeting.getOrganizerId().equals(currentUserId)) {
            return Result.error("只有会议发起者可以添加管理员");
        }
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 检查用户是否已经是该会议的参与者
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId);
        MeetingParticipant participant = participantMapper.selectOne(wrapper);

        if (participant != null) {
            // 如果已经是管理员
            if (ParticipantRole.ADMIN.equals(participant.getRole())) {
                return Result.error("该用户已经是此会议的管理员");
            }
            // 如果是其他角色，更新为管理员
            participant.setRole(ParticipantRole.ADMIN);
            participantMapper.updateById(participant);
        } else {
            // 如果不是参与者，创建新记录
            participant = new MeetingParticipant();
            participant.setMeetingId(meetingId);
            participant.setUserId(userId);
            participant.setRole(ParticipantRole.ADMIN);
            participant.setSignInStatus("未签到");
            participant.setLeaveStatus("正常");
            participant.setLastActiveTime(new Date());
            participantMapper.insert(participant);
        }

        return Result.success("添加管理员成功");
    }
    @Override
    @Transactional
    public Result<?> endMeeting(Long meetingId, Long userId) {
        try {
            // 1. 查询会议
            Meeting meeting = meetingMapper.selectById(meetingId);
            if (meeting == null) {
                return Result.error("会议不存在");
            }

            // 2. 验证是否为会议发起者
            if (!meeting.getOrganizerId().equals(userId)) {
                return Result.error("只有会议发起者可以结束会议");
            }

            // 3. 更新会议状态
            meeting.setStatus("已结束");
            meeting.setEndTime(LocalDateTime.now());
            meetingMapper.updateById(meeting);

            // 4. 获取用户名
            User user = userMapper.selectById(userId);
            String username = user != null ? user.getUsername() : "未知用户";
            // 5. 发送系统消息
            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setMeetingId(meetingId);
            systemMessage.setUserId(userId);
            systemMessage.setMessageType("SYSTEM");
            systemMessage.setContent(String.format("userId:%d, username:%s, action:结束会议", userId, username));
            systemMessage.setSendTime(LocalDateTime.now());
            systemMessage.setUpdateTime(LocalDateTime.now());
            systemMessage.setSenderName("系统");
            chatMessageMapper.insert(systemMessage);

            return Result.success("会议已成功结束");
        } catch (Exception e) {
            log.error("结束会议失败", e);
            return Result.error("结束会议失败：" + e.getMessage());
        }
    }
    @Override
    public Result<?> removeMeetingAdmin(Long meetingId, Long userId, Long currentUserId) {
        if (meetingId == null || userId == null || currentUserId == null) {
            return Result.error("参数不能为空");
        }

        // 检查会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }

        // 检查当前操作用户是否是会议发起者
        if (!meeting.getOrganizerId().equals(currentUserId)) {
            return Result.error("只有会议发起者可以移除管理员");
        }

        // 检查用户是否是管理员
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId)
                .eq(MeetingParticipant::getRole, ParticipantRole.ADMIN);
        MeetingParticipant participant = participantMapper.selectOne(wrapper);

        if (participant == null) {
            return Result.error("该用户不是此会议的管理员");
        }

        // 将管理员角色改为普通参与者
        participant.setRole(ParticipantRole.PARTICIPANT);
        participantMapper.updateById(participant);

        return Result.success("移除管理员成功");
    }
    @Override
    public Result<?> getMeetingManagers(Long meetingId) {
        // 检查会议是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null) {
            return Result.error("会议不存在");
        }
        // 查询会议的管理者列表
        List<Map<String, Object>> managers = meetingMapper.selectMeetingManagers(meetingId);
        // 将结果分类
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("hosts", managers.stream()
                .filter(m -> "HOST".equals(m.get("role")))
                .collect(Collectors.toList()));
        result.put("admins", managers.stream()
                .filter(m -> "ADMIN".equals(m.get("role")))
                .collect(Collectors.toList()));

        return Result.success(result);
    }
    @Override
    public List<UserMeetingParticipationDTO> getUserParticipationHistory(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        // 构建查询条件
        LambdaQueryWrapper<MeetingParticipation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipation::getUserId, userId);

        // 如果提供了时间范围
        if (startDate != null || endDate != null) {
            wrapper.and(w -> {
                if (startDate != null) {
                    w.ge(MeetingParticipation::getJoinTime, startDate);
                }
                if (endDate != null) {
                    w.le(MeetingParticipation::getLeaveTime, endDate);
                }
            });
        }

        wrapper.orderByDesc(MeetingParticipation::getJoinTime);

        // 查询参会记录
        List<MeetingParticipation> participations = participationMapper.selectList(wrapper);
        List<UserMeetingParticipationDTO> result = new ArrayList<>();

        // 转换数据
        for (MeetingParticipation participation : participations) {
            Meeting meeting = meetingMapper.selectById(participation.getMeetingId());
            if (meeting != null) {
                UserMeetingParticipationDTO dto = new UserMeetingParticipationDTO();
                BeanUtils.copyProperties(meeting, dto);
                // 设置会议开始和结束时间
                dto.setStartTime(meeting.getStartTime());
                dto.setEndTime(meeting.getEndTime());

                dto.setJoinTime(participation.getJoinTime());
                dto.setLeaveTime(participation.getLeaveTime());

                // 设置签到状态
                LambdaQueryWrapper<MeetingParticipant> participantWrapper = new LambdaQueryWrapper<>();
                participantWrapper.eq(MeetingParticipant::getMeetingId, participation.getMeetingId())
                        .eq(MeetingParticipant::getUserId, userId);
                MeetingParticipant participant = participantMapper.selectOne(participantWrapper);

                if (participant != null) {
                    dto.setSignInStatus(participant.getSignInStatus());
                } else {
                    dto.setSignInStatus("未签到");
                }
                // 计算参会时长
                if (participation.getLeaveTime() != null) {
                    long totalMinutes = ChronoUnit.MINUTES.between(
                            participation.getJoinTime(),
                            participation.getLeaveTime()
                    );
                    long hours = totalMinutes / 60;
                    long minutes = totalMinutes % 60;
                    dto.setDuration(totalMinutes);
                    dto.setDurationDisplay(String.format("%d小时%d分钟", hours, minutes));
                }
                result.add(dto);
            }
        }

        return result;
    }
}