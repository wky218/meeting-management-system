package com.cms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.dto.MeetingSearchDTO;
import com.cms.pojo.Meeting;
import com.cms.pojo.User;
import com.cms.vo.MeetingVO;
import com.cms.vo.ParticipantVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MeetingMapper extends BaseMapper<Meeting> {
    @Select("SELECT m.* FROM meetings m WHERE DATE(m.start_time) = CURDATE() ORDER BY m.start_time")
    List<MeetingVO> getTodayMeetings();

    @Select("SELECT m.* FROM meetings m " +
            "WHERE m.start_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 MINUTE) " +
            "ORDER BY m.start_time")
    List<MeetingVO> getUpcomingMeetings();

    @Select("SELECT m.* FROM meetings m " +
            "INNER JOIN meeting_participants mp ON m.meeting_id = mp.meeting_id " +
            "WHERE mp.participant_id = #{userId} " +
            "ORDER BY m.start_time DESC")
    List<MeetingVO> getUserMeetings(@Param("userId") Long userId);
    @Select("SELECT m.*, mp.participant_id " +
            "FROM meetings m " +
            "INNER JOIN meeting_participants mp ON m.meeting_id = mp.meeting_id " +
            "WHERE mp.participant_id = #{userId} " +
            "AND DATE(m.start_time) = CURDATE() " +
            "ORDER BY m.start_time ASC")
    List<MeetingVO> getUserTodayMeetings(@Param("userId") Long userId);

    @Select("SELECT m.*, mp.participant_id " +
            "FROM meetings m " +
            "INNER JOIN meeting_participants mp ON m.meeting_id = mp.meeting_id " +
            "WHERE mp.participant_id = #{userId} " +
            "AND m.start_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY m.start_time ASC")
    List<MeetingVO> getUserUpcomingMeetings(@Param("userId") Long userId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
    @Select("SELECT u.* FROM users u " +
            "INNER JOIN meeting_participants mp ON u.user_id = mp.participant_id " +
            "WHERE mp.meeting_id = #{meetingId}")
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "username", column = "username"),
            @Result(property = "name", column = "name"),
            @Result(property = "email", column = "email"),
            @Result(property = "phone", column = "phone")
    })
    List<User> getMeetingParticipants(@Param("meetingId") Long meetingId);

    @Select("SELECT u.user_id, u.username " +
            "FROM users u " +
            "INNER JOIN meeting_participants mp ON u.user_id = mp.user_id " +
            "WHERE mp.meeting_id = #{meetingId}")
    List<ParticipantVO> getMeetingParticipantsList(@Param("meetingId") Long meetingId);


    @Insert("<script>" +
            "INSERT INTO meeting_admins(id, meeting_id, admin_id) VALUES " +
            "<foreach collection='adminIds' item='adminId' separator=','>" +
            "(#{id}, #{meetingId}, #{adminId})" +
            "</foreach>" +
            "</script>")
    void insertMeetingAdmins(@Param("meetingId") Long meetingId, @Param("adminIds") List<Long> adminIds);

    @Select("SELECT admin_id FROM meeting_admins WHERE meeting_id = #{meetingId}")
    List<Long> getMeetingAdminIds(@Param("meetingId") Long meetingId);

    @Select("<script>" +
            "SELECT * FROM meetings " +
            "WHERE 1=1 " +
            "<if test='meetingId != null and meetingId != \"\"'>" +
            "   AND meeting_id = #{meetingId} " +
            "</if>" +
            "<if test='title != null and title != \"\"'>" +
            "   AND title LIKE CONCAT('%', #{title}, '%') " +
            "</if>" +
            "<if test='meetingType != null and meetingType != \"\"'>" +
            "   AND meeting_type LIKE CONCAT('%', #{meetingType}, '%') " +
            "</if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<Meeting> searchMeetings(MeetingSearchDTO searchDTO);

    @Select("SELECT p.user_id FROM meeting_participants p WHERE p.meeting_id = #{meetingId}")
    List<Long> getParticipantIds(Long meetingId);
    @Select("SELECT * FROM meetings " +
            "WHERE visibility = 'PUBLIC' " +
            "AND status = '待开始' " +
            "ORDER BY created_at DESC")
    List<Meeting> findPublicMeetings();

    @Select("SELECT m.* FROM meetings m " +
            "INNER JOIN meeting_participants mp ON m.meeting_id = mp.meeting_id " +
            "WHERE m.visibility = 'PRIVATE' AND mp.user_id = #{userId} " +
            "ORDER BY m.created_at DESC")
    List<Meeting> findUserPrivateMeetings(@Param("userId") Long userId);
    @Select("SELECT u.user_id, u.username, mp.role " +
            "FROM meeting_participants mp " +
            "INNER JOIN users u ON mp.user_id = u.user_id " +
            "WHERE mp.meeting_id = #{meetingId} " +
            "AND mp.role IN ('HOST', 'ADMIN')")
    List<Map<String, Object>> selectMeetingManagers(Long meetingId);

    //查所有参与者，包含管理员和发起者、普通会员
    @Select("SELECT u.user_id, u.username, mp.role " +
            "FROM meeting_participants mp " +
            "INNER JOIN users u ON mp.user_id = u.user_id " +
            "WHERE mp.meeting_id = #{meetingId} " +
            "ORDER BY FIELD(mp.role, 'HOST', 'ADMIN', 'PARTICIPANT')")

    List<Map<String, Object>> selectAllMeetingParticipants(Long meetingId);
    @Update("UPDATE meeting_participants SET role = 'PARTICIPANT' " +
            "WHERE meeting_id = #{meetingId} AND user_id = #{userId} AND role = 'ADMIN'")
    int removeMeetingAdmin(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    @Select("SELECT u.user_id, u.username, mp.role, mp.sign_in_status, mp.leave_status " +
            "FROM meeting_participants mp " +
            "INNER JOIN users u ON mp.user_id = u.user_id " +
            "WHERE mp.meeting_id = #{meetingId} " +
            "ORDER BY FIELD(mp.role, 'HOST', 'ADMIN', 'PARTICIPANT')")
    List<Map<String, Object>> selectAllMeetingParticipantsWithStatus(Long meetingId);
}