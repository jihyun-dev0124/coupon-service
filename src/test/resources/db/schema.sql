CREATE TABLE IF NOT EXISTS tb_coupon_issue (
  id BIGINT NOT NULL AUTO_INCREMENT,
  coupon_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  issued_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_coupon_user (coupon_id, user_id),
  KEY idx_coupon_id (coupon_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tb_outbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  topic VARCHAR(128) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  payload LONGTEXT NOT NULL,
  status VARCHAR(16) NOT NULL,
  fail_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL,
  published_at TIMESTAMP(6) NULL,
  version BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbox_event_id (event_id),
  KEY idx_outbox_status_id (status, id),
  KEY idx_outbox_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
