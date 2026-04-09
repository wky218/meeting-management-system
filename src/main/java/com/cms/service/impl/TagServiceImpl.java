package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cms.common.Result;
import com.cms.mapper.TagMapper;
import com.cms.mapper.MeetingMapper;
import com.cms.mapper.UserTagRelationMapper;
import com.cms.mapper.MeetingTagRelationMapper;
import com.cms.pojo.Meeting;
import com.cms.pojo.Tag;
import com.cms.pojo.UserTagRelation;
import com.cms.pojo.MeetingTagRelation;
import com.cms.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final UserTagRelationMapper userTagRelationMapper;
    private final MeetingTagRelationMapper meetingTagRelationMapper;
    private final MeetingMapper meetingMapper;
    @Override
    @Transactional
    public Result<?> createTag(String tagName) {
        try {
            Tag existingTag = tagMapper.findByTagName(tagName);
            if (existingTag != null) {
                return Result.success(existingTag);
            }
            tagMapper.createTag(tagName);
            return Result.success("标签创建成功");
        } catch (Exception e) {
            log.error("创建标签失败", e);
            return Result.error("创建标签失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<?> deleteTag(Long tagId) {
        try {
            tagMapper.deleteById(tagId);
            return Result.success("标签删除成功");
        } catch (Exception e) {
            log.error("删除标签失败", e);
            return Result.error("删除标签失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> getAllTags() {
        try {
            List<Tag> tags = tagMapper.selectList(null);
            return Result.success(tags);
        } catch (Exception e) {
            log.error("获取所有标签失败", e);
            return Result.error("获取标签失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<?> addUserTagByName(Long userId, String tagName) {
        try {
            // 先确保标签存在
            Tag tag = tagMapper.findByTagName(tagName);
            if (tag == null) {
                tagMapper.createTag(tagName);
                tag = tagMapper.findByTagName(tagName);
            }

            UserTagRelation relation = new UserTagRelation();
            relation.setUserId(userId);
            relation.setTagId(tag.getId());
            userTagRelationMapper.insert(relation);

            return Result.success("用户标签添加成功");
        } catch (Exception e) {
            log.error("添加用户标签失败", e);
            return Result.error("添加用户标签失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<?> removeUserTag(Long userId, Long tagId) {
        try {
            userTagRelationMapper.removeRelation(userId, tagId);
            return Result.success("用户标签移除成功");
        } catch (Exception e) {
            log.error("移除用户标签失败", e);
            return Result.error("移除用户标签失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> getUserTags(Long userId) {
        try {
            List<Tag> tags = userTagRelationMapper.findTagsByUserId(userId);
            return Result.success(tags);
        } catch (Exception e) {
            log.error("获取用户标签失败", e);
            return Result.error("获取用户标签失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> addMeetingTagByName(Long meetingId, String tagName) {
        try {
            // 检查会议是否存在
            Meeting meeting = meetingMapper.selectById(meetingId);
            if (meeting == null) {
                return Result.error("会议不存在");
            }

            // 获取会议类型对应的标签
            String meetingType = meeting.getMeetingType();
            if (meetingType == null || meetingType.trim().isEmpty()) {
                return Result.error("会议类型不能为空");
            }

            // 通过会议类型查找对应的标签
            Tag tag = tagMapper.findByTagName(meetingType);
            if (tag == null) {
                return Result.error("未找到对应会议类型的标签：" + meetingType);
            }

            // 检查关联是否已存在
            MeetingTagRelation existingRelation = meetingTagRelationMapper.selectOne(
                    new QueryWrapper<MeetingTagRelation>()
                            .eq("meeting_id", meetingId)
                            .eq("tag_id", tag.getId())
            );

            if (existingRelation != null) {
                return Result.error("该会议类型标签已经添加到会议中");
            }

            // 创建新的关联
            MeetingTagRelation relation = new MeetingTagRelation();
            relation.setMeetingId(meetingId);
            relation.setTagId(tag.getId());
            meetingTagRelationMapper.insert(relation);

            return Result.success("会议类型标签关联成功");
        } catch (Exception e) {
            return Result.error("添加会议类型标签失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<?> removeMeetingTag(Long meetingId, Long tagId) {
        try {
            meetingTagRelationMapper.removeRelation(meetingId, tagId);
            return Result.success("会议标签移除成功");
        } catch (Exception e) {
            log.error("移除会议标签失败", e);
            return Result.error("移除会议标签失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> getMeetingTags(Long meetingId) {
        try {
            List<Tag> tags = meetingTagRelationMapper.findTagsByMeetingId(meetingId);
            return Result.success(tags);
        } catch (Exception e) {
            log.error("获取会议标签失败", e);
            return Result.error("获取会议标签失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> batchAddMeetingTags(Long meetingId, List<String> tagNames) {
        try {
            // 检查会议是否存在
            Meeting meeting = meetingMapper.selectById(meetingId);
            if (meeting == null) {
                return Result.error("会议不存在");
            }

            for (String tagName : tagNames) {
                // 通过标签名查找标签
                Tag tag = tagMapper.findByTagName(tagName);
                if (tag == null) {
                    return Result.error("标签不存在：" + tagName);
                }

                // 检查关联是否已存在
                MeetingTagRelation existingRelation = meetingTagRelationMapper.selectOne(
                        new QueryWrapper<MeetingTagRelation>()
                                .eq("meeting_id", meetingId)
                                .eq("tag_id", tag.getId())
                );

                if (existingRelation != null) {
                    return Result.error("标签已经添加到会议中：" + tagName);
                }

                // 创建新的关联
                MeetingTagRelation relation = new MeetingTagRelation();
                relation.setMeetingId(meetingId);
                relation.setTagId(tag.getId());
                meetingTagRelationMapper.insert(relation);
            }

            return Result.success("批量添加标签成功");
        } catch (Exception e) {
            log.error("批量添加会议标签失败", e);
            return Result.error("批量添加会议标签失败：" + e.getMessage());
        }
    }


    @Override
    @Transactional
    public Result<?> batchAddUserTags(Long userId, List<String> tagNames) {
        try {
            for (String tagName : tagNames) {
                addUserTagByName(userId, tagName);
            }
            return Result.success("批量添加用户标签成功");
        } catch (Exception e) {
            log.error("批量添加用户标签失败", e);
            return Result.error("批量添加用户标签失败：" + e.getMessage());
        }
    }
}