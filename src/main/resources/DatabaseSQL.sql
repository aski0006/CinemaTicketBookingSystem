-- 影院订票系统数据库结构（适配Redis为主的场次与座位存储方案）
CREATE DATABASE IF NOT EXISTS cinema_ticket_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cinema_ticket_db;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE,
    avatar VARCHAR(255),
    member_level SMALLINT DEFAULT 0,
    status ENUM('ACTIVE','INACTIVE','LOCKED','ADMIN') DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    member_expire_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 影片表
CREATE TABLE IF NOT EXISTS movie (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    director VARCHAR(50),
    actors VARCHAR(200),
    duration SMALLINT NOT NULL,
    release_date DATE NOT NULL,
    description TEXT,
    poster_url VARCHAR(255),
    trailer_url VARCHAR(255),
    rating DECIMAL(3,1) DEFAULT 0.0,
    genre VARCHAR(50) NOT NULL DEFAULT '',
    status ENUM('UPCOMING','SHOWING','OFF') DEFAULT 'UPCOMING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 影厅表
CREATE TABLE IF NOT EXISTS hall (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type ENUM('STANDARD','IMAX','DOLBY','VIP') DEFAULT 'STANDARD',
    capacity SMALLINT NOT NULL,
    seat_layout LONGTEXT NOT NULL, -- 存储座位布局JSON
    screen_type VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 座位表
CREATE TABLE IF NOT EXISTS `seat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `col_no` int DEFAULT NULL,
  `hall_id` bigint DEFAULT NULL,
  `price_factor` double DEFAULT NULL,
  `row_no` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 场次表
CREATE TABLE IF NOT EXISTS session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    price DECIMAL(8,2) NOT NULL,
    available_seats SMALLINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING_PAYMENT','COMPLETED','CANCELLED','REFUNDED','FAILED') DEFAULT 'PENDING_PAYMENT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    payment_time DATETIME,
    e_ticket_url VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单座位快照表（不依赖seat_id，存row/col/type/price_factor/final_price/status）
CREATE TABLE IF NOT EXISTS order_seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    row_no INT NOT NULL,
    col_no INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    price_factor DECIMAL(3,2) DEFAULT 1.0,
    final_price DECIMAL(8,2) NOT NULL,
    status ENUM('AVAILABLE','LOCKED','OCCUPIED') DEFAULT 'LOCKED',
    FOREIGN KEY (order_id) REFERENCES `order`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 影评表
CREATE TABLE IF NOT EXISTS review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT,
    images JSON,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 统计表
CREATE TABLE IF NOT EXISTS statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL,
    movie_id BIGINT,
    session_id BIGINT,
    ticket_sales INT DEFAULT 0,
    revenue DECIMAL(12,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会员订单表
CREATE TABLE IF NOT EXISTS membership_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    membership_type ENUM('VIP','SVIP') NOT NULL,
    duration ENUM('monthly','quarterly','yearly') NOT NULL,
    payment_method ENUM('ALIPAY','WECHAT','CREDIT_CARD') NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING_PAYMENT','COMPLETED','FAILED') DEFAULT 'PENDING_PAYMENT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    payment_time DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    refund_amount DECIMAL(10,2),
    refund_status ENUM('NONE','REQUESTED','REFUNDED','REJECTED') DEFAULT 'NONE',
    refund_time DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 外键约束
ALTER TABLE session ADD CONSTRAINT fk_session_movie FOREIGN KEY (movie_id) REFERENCES movie(id);
ALTER TABLE session ADD CONSTRAINT fk_session_hall FOREIGN KEY (hall_id) REFERENCES hall(id);
ALTER TABLE `order` ADD CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE `order` ADD CONSTRAINT fk_order_session FOREIGN KEY (session_id) REFERENCES session(id);
ALTER TABLE review ADD CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE review ADD CONSTRAINT fk_review_movie FOREIGN KEY (movie_id) REFERENCES movie(id);
ALTER TABLE statistics ADD CONSTRAINT fk_statistics_movie FOREIGN KEY (movie_id) REFERENCES movie(id);
ALTER TABLE statistics ADD CONSTRAINT fk_statistics_session FOREIGN KEY (session_id) REFERENCES session(id);