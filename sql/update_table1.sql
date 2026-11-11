ALTER TABLE user
    ADD COLUMN lastLoginTime DATETIME NULL COMMENT '最近一次成功登录时间',
    ADD COLUMN lastLoginIp VARCHAR(45) NULL COMMENT '最近一次登录的请求来源 IP',
    ADD COLUMN lastLoginUserAgent VARCHAR(512) NULL COMMENT '最近一次登录的 User-Agent';

SHOW CREATE TABLE user;

ALTER TABLE user
    MODIFY COLUMN updateTime DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        COMMENT '用户资料更新时间';