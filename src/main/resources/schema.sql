-- 创建标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag_name VARCHAR(50) NOT NULL UNIQUE
);

-- 创建用户标签关系表
CREATE TABLE IF NOT EXISTS user_tag_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (tag_id) REFERENCES tags(id),
    UNIQUE KEY unique_user_tag (user_id, tag_id)
);

-- 创建会议标签关系表
CREATE TABLE IF NOT EXISTS meeting_tag_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id),
    FOREIGN KEY (tag_id) REFERENCES tags(id),
    UNIQUE KEY unique_meeting_tag (meeting_id, tag_id)
); 