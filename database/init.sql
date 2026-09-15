CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nickname VARCHAR(80) NOT NULL,
  password_hash VARCHAR(128),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_nickname (nickname)
);

-- created_by 允许为 NULL：升级前创建的旧宝宝没有创建者归属，等待认领
CREATE TABLE IF NOT EXISTS baby (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  birthday DATE NOT NULL,
  blood_type VARCHAR(10),
  initial_height DECIMAL(5,2),
  initial_weight DECIMAL(5,2),
  created_by BIGINT
);

-- 家庭成员：角色 OWNER(创建者) / MANAGE(管理) / RECORD(记录) / VIEW(查看)
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

-- 一次性邀请码：status ACTIVE / REVOKED / CLAIMED，过期由 expires_at 判定
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

CREATE TABLE IF NOT EXISTS growth_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  recorded_at DATE NOT NULL,
  height_cm DECIMAL(5,2),
  weight_kg DECIMAL(5,2),
  percentile VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS vaccine_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  vaccine_name VARCHAR(120) NOT NULL,
  planned_date DATE NOT NULL,
  completed BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS food_recipe (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  month_age_min INT NOT NULL,
  month_age_max INT NOT NULL,
  name VARCHAR(120) NOT NULL,
  ingredients TEXT,
  steps TEXT,
  nutrition TEXT,
  allergens VARCHAR(160)
);
