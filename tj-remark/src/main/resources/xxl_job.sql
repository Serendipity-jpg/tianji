CREATE TABLE `xxl_job_qrtz_lock` (
	`lock_name` VARCHAR ( 64 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`lock_value` VARCHAR ( 64 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`lock_grant` VARCHAR ( 64 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`lock_thread` VARCHAR ( 255 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	PRIMARY KEY ( `lock_name` ),
	UNIQUE KEY `idx_lock_name` ( `lock_name` )
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE TABLE `xxl_job_qrtz_triggers` (
	`trigger_name` VARCHAR ( 80 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`trigger_group` VARCHAR ( 80 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`job_name` VARCHAR ( 80 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`job_group` VARCHAR ( 80 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`next_fire_time` BIGINT ( 13 ) DEFAULT NULL,
	`prev_fire_time` BIGINT ( 13 ) DEFAULT NULL,
	`priority` INTEGER ( 11 ) DEFAULT NULL,
	`trigger_state` VARCHAR ( 16 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`trigger_type` VARCHAR ( 8 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`start_time` BIGINT ( 13 ) NOT NULL,
	`end_time` BIGINT ( 13 ) DEFAULT NULL,
	`calendar_name` VARCHAR ( 255 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`misfire_instr` INTEGER ( 11 ) DEFAULT NULL,
	`job_data` BLOB,
	PRIMARY KEY ( `trigger_name`, `trigger_group` ),
	FOREIGN KEY ( `job_name`, `job_group` ) REFERENCES `xxl_job

	_qrtz_jobs` ( `job_name`, `job_group` ) ON DELETE CASCADE
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE TABLE `xxl_job_qrtz_jobs` (
	`job_name` VARCHAR ( 80 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`job_group` VARCHAR ( 80 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`desc` VARCHAR ( 100 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`add_time` BIGINT ( 13 ) DEFAULT NULL,
	`update_time` BIGINT ( 13 ) DEFAULT NULL,
	`author` VARCHAR ( 100 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`alarm_email` VARCHAR ( 100 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`executor_route_strategy` VARCHAR ( 10 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`executor_handler` VARCHAR ( 255 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`executor_param` VARCHAR ( 512 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`executor_block_strategy` VARCHAR ( 10 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`executor_fail_retry_count` INT ( 11 ) DEFAULT NULL,
	`child_jobid` VARCHAR ( 255 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`trigger_status` VARCHAR ( 10 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	PRIMARY KEY ( `job_name`, `job_group` ),
	KEY `idx_qrtz_jobs_group` ( `job_group` ),
	FOREIGN KEY ( `job_group` ) REFERENCES `xxl_job_qrtz_job_groups` ( `job_group` ) ON DELETE CASCADE
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
CREATE TABLE `xxl_job_qrtz_job_groups` (
	`job_group` VARCHAR ( 80 ) COLLATE utf8mb4_unicode_ci NOT NULL,
	`desc` VARCHAR ( 100 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
	`add_time` BIGINT ( 13 ) DEFAULT NULL,
	`update_time` BIGINT ( 13 ) DEFAULT NULL,
	`author` VARCHAR ( 100 ) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
PRIMARY KEY ( `job_group` )
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;