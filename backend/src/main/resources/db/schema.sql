CREATE TABLE IF NOT EXISTS sql_ticket (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id varchar(50) NOT NULL,
  instance_id bigint NOT NULL,
  type varchar(20) NOT NULL,
  reason varchar(255),
  sql_text text,
  file_url varchar(255),
  status varchar(20) NOT NULL,
  submitter_id bigint,
  submitter_id_card varchar(50),
  submit_time datetime,
  process_instance_id varchar(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sql_ticket_detail (
  id bigint NOT NULL PRIMARY KEY,
  tenant_id varchar(50) NOT NULL,
  ticket_id bigint NOT NULL,
  sql_text text,
  attachment_oss_key varchar(255),
  affect_rows_estimate int
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS db_instance (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id varchar(50) NOT NULL,
  name varchar(100),
  db_type varchar(50),
  jdbc_url varchar(255),
  read_only_jdbc_url varchar(255),
  username varchar(100),
  password_cipher varchar(255),
  env varchar(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sql_audit_log (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id varchar(50) NOT NULL,
  ticket_id bigint,
  execute_sql text,
  cost_time_ms bigint,
  status varchar(20),
  error_trace text,
  previous_hash varchar(255),
  current_hash varchar(255),
  rollback_sql text
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
