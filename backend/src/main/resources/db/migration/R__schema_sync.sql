-- Repeatable migration:
-- Keep this file aligned with backend data contracts.
-- Flyway reruns this script whenever content changes.

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  display_name VARCHAR(64),
  avatar_url LONGTEXT,
  gender VARCHAR(16),
  phone VARCHAR(32),
  balance DECIMAL(12, 2) NOT NULL DEFAULT 0,
  identity_status VARCHAR(32) NOT NULL DEFAULT '未认证',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS user_addresses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  address VARCHAR(255) NOT NULL,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  is_default VARCHAR(8) NOT NULL DEFAULT '否',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

