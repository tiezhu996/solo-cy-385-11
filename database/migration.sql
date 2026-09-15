-- 升级脚本：在已有数据的部署上执行一次（可重复执行）。
-- 用法：docker compose exec -T db mysql -ubabytracker -pbabytracker_pass babytracker < database/migration.sql
-- 说明：旧宝宝 created_by 为 NULL，视为“待认领”，任一登录用户认领后成为创建者（OWNER）；
--       旧账号 password_hash 为 NULL，需先通过 /api/users/set-password 设置初始密码再登录。

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nickname VARCHAR(80) NOT NULL,
  password_hash VARCHAR(128),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_nickname (nickname)
);

CREATE TABLE IF NOT EXISTS baby_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_baby_user (baby_id, user_id),
  KEY idx_member_user (user_id)
);

CREATE TABLE IF NOT EXISTS baby_invite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  expires_at DATETIME NOT NULL,
  created_by BIGINT NOT NULL,
  claimed_by BIGINT,
  claimed_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_invite_code (code),
  KEY idx_invite_baby (baby_id)
);

-- baby.created_by：不存在则新增；已存在则放宽为可空（旧行允许 NULL 归属）
SET @has_created_by := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'baby' AND COLUMN_NAME = 'created_by'
);
SET @ddl := IF(@has_created_by = 0,
  'ALTER TABLE baby ADD COLUMN created_by BIGINT NULL',
  'ALTER TABLE baby MODIFY COLUMN created_by BIGINT NULL');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- app_user.password_hash：不存在则新增（老版本可能连 app_user 都没有，上面已建）
SET @has_password := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'password_hash'
);
SET @ddl := IF(@has_password = 0,
  'ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(128) NULL',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
