CREATE DATABASE IF NOT EXISTS cinema_ticket_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cinema_ticket_db;

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
    status ENUM('UPCOMING','SHOWING','OFF') DEFAULT 'UPCOMING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hall (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type ENUM('STANDARD','IMAX','DOLBY','VIP') DEFAULT 'STANDARD',
    capacity SMALLINT NOT NULL,
    seat_layout TEXT NOT NULL,
    screen_type VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    price DECIMAL(8,2) NOT NULL,
    available_seats SMALLINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hall_id BIGINT NOT NULL,
    row_no CHAR(2) NOT NULL,
    col_no TINYINT NOT NULL,
    type ENUM('STANDARD','VIP','COUPLE') DEFAULT 'STANDARD',
    price_factor DECIMAL(3,2) DEFAULT 1.0,
    status ENUM('AVAILABLE','LOCKED','OCCUPIED') DEFAULT 'AVAILABLE',
    UNIQUE KEY uk_hall_seat (hall_id, row_no, col_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(20) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING_PAYMENT','COMPLETED','CANCELLED','REFUNDED') DEFAULT 'PENDING_PAYMENT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    payment_time DATETIME,
    e_ticket_url VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    final_price DECIMAL(8,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT,
    images JSON,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL,
    movie_id BIGINT,
    session_id BIGINT,
    ticket_sales INT DEFAULT 0,
    revenue DECIMAL(12,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    payment_url VARCHAR(255),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    refund_amount DECIMAL(10,2),
    refund_status ENUM('NONE','REQUESTED','REFUNDED','REJECTED') DEFAULT 'NONE',
    refund_time DATETIME,
    CONSTRAINT fk_membership_order_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 外键约束统一添加
ALTER TABLE session ADD CONSTRAINT fk_session_movie FOREIGN KEY (movie_id) REFERENCES movie(id);
ALTER TABLE session ADD CONSTRAINT fk_session_hall FOREIGN KEY (hall_id) REFERENCES hall(id);

ALTER TABLE seat ADD CONSTRAINT fk_seat_hall FOREIGN KEY (hall_id) REFERENCES hall(id);

ALTER TABLE `order` ADD CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE `order` ADD CONSTRAINT fk_order_session FOREIGN KEY (session_id) REFERENCES session(id);

ALTER TABLE order_seat ADD CONSTRAINT fk_order_seat_order FOREIGN KEY (order_id) REFERENCES `order`(id);
ALTER TABLE order_seat ADD CONSTRAINT fk_order_seat_seat FOREIGN KEY (seat_id) REFERENCES seat(id);

ALTER TABLE review ADD CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE review ADD CONSTRAINT fk_review_movie FOREIGN KEY (movie_id) REFERENCES movie(id);

ALTER TABLE statistics ADD CONSTRAINT fk_statistics_movie FOREIGN KEY (movie_id) REFERENCES movie(id);
ALTER TABLE statistics ADD CONSTRAINT fk_statistics_session FOREIGN KEY (session_id) REFERENCES session(id);