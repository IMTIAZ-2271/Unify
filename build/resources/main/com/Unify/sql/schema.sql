CREATE DATABASE IF NOT EXISTS calendar_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE calendar_app;

-- =========================================================================
-- 1. CORE APP TABLES
-- =========================================================================

CREATE TABLE IF NOT EXISTS users (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    bio TEXT,
    profile_picture LONGBLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
    );

CREATE TABLE IF NOT EXISTS groups_tbl (
                                          id INT AUTO_INCREMENT PRIMARY KEY,
                                          group_code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    profile_picture LONGBLOB,
    parent_group_id INT NULL,
    created_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_group_id) REFERENCES groups_tbl(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_groups_sync (last_modified)
    );

CREATE TABLE IF NOT EXISTS group_members (
                                             id INT AUTO_INCREMENT PRIMARY KEY,
                                             group_id INT NOT NULL,
                                             user_id INT NOT NULL,
                                             role ENUM('admin','member') DEFAULT 'member',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_member (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups_tbl(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS events (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      title VARCHAR(200) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    event_type ENUM('personal','group') NOT NULL DEFAULT 'personal',
    group_id INT NULL,
    created_by INT NOT NULL,
    color VARCHAR(7) DEFAULT '#3B82F6',
    is_all_day BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups_tbl(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_events_sync (created_by, last_modified)
    );

CREATE TABLE IF NOT EXISTS join_requests (
                                             id INT AUTO_INCREMENT PRIMARY KEY,
                                             group_id INT NOT NULL,
                                             user_id INT NOT NULL,
                                             status ENUM('pending','accepted','declined') DEFAULT 'pending',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    responded_by INT NULL,
    UNIQUE KEY uq_request (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups_tbl(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS notifications (
                                             id INT AUTO_INCREMENT PRIMARY KEY,
                                             user_id INT NOT NULL,
                                             title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'info',
    is_read BOOLEAN DEFAULT FALSE,
    reference_id INT NULL,
    invite_accepted INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_sync (user_id, last_modified),
    INDEX idx_notif_badge (user_id, is_read)
    );

CREATE TABLE IF NOT EXISTS canteens_tbl (
                                            id INT AUTO_INCREMENT PRIMARY KEY,
                                            group_id INT NOT NULL,
                                            name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups_tbl(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS canteen_foods_tbl (
                                                 id INT AUTO_INCREMENT PRIMARY KEY,
                                                 canteen_id INT NOT NULL,
                                                 name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    available_qty INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (canteen_id) REFERENCES canteens_tbl(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS canteen_orders_tbl (
                                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                                  user_id INT NOT NULL,
                                                  food_id INT NOT NULL,
    order_batch_id VARCHAR(64) NULL,
    status ENUM('pending','collected','received','cancelled') DEFAULT 'pending',
    order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (food_id) REFERENCES canteen_foods_tbl(id) ON DELETE CASCADE,
    INDEX idx_canteen_orders_user (user_id, status),
    INDEX idx_canteen_orders_batch (order_batch_id),
    INDEX idx_canteen_orders_time (order_time)
    );


-- =========================================================================
-- 2. SYNC ENGINE & UTILITY TABLES (LIBRARY)
-- =========================================================================

CREATE TABLE IF NOT EXISTS deleted_records (
                                               id INT AUTO_INCREMENT PRIMARY KEY,
                                               table_name VARCHAR(50) NOT NULL,
    record_id INT NOT NULL,
    deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_deleted_sync (deleted_at)
    );

CREATE TABLE IF NOT EXISTS books (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     group_id INT NOT NULL,
                                     title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    description TEXT,
    total_copies INT DEFAULT 1,
    available_copies INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups_tbl(id) ON DELETE CASCADE,
    INDEX idx_library_sync (group_id, title)
    );

CREATE TABLE IF NOT EXISTS book_issues (
                                           id INT AUTO_INCREMENT PRIMARY KEY,
                                           book_id INT NOT NULL,
                                           user_id INT NOT NULL,
                                           status ENUM('pending','issued','returned','rejected') DEFAULT 'pending',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    issued_at TIMESTAMP NULL,
    due_date TIMESTAMP NULL,
    returned_at TIMESTAMP NULL,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );


-- =========================================================================
-- 3. DEFAULT ADMIN ACCOUNT
-- =========================================================================

-- Default admin user (password = admin123)
INSERT IGNORE INTO users (username, email, password_hash, display_name)
VALUES ('root','admin@app.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','Administrator');


-- =========================================================================
-- 4. TRIGGERS (FOR REAL-TIME DELETION SYNCING)
-- =========================================================================

DELIMITER //

DROP TRIGGER IF EXISTS after_event_delete //
CREATE TRIGGER after_event_delete
    AFTER DELETE ON events
    FOR EACH ROW BEGIN
    INSERT INTO deleted_records (table_name, record_id) VALUES ('events', OLD.id);
END//

DROP TRIGGER IF EXISTS after_group_delete //
CREATE TRIGGER after_group_delete
    AFTER DELETE ON groups_tbl
    FOR EACH ROW BEGIN
    INSERT INTO deleted_records (table_name, record_id) VALUES ('groups_tbl', OLD.id);
END//

DROP TRIGGER IF EXISTS after_notification_delete //
CREATE TRIGGER after_notification_delete
    AFTER DELETE ON notifications
    FOR EACH ROW BEGIN
    INSERT INTO deleted_records (table_name, record_id) VALUES ('notifications', OLD.id);
END//

DELIMITER ;
