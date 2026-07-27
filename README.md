mysql workbench所需操作：
运行这行代码来创建数据库：CREATE DATABASE IF NOT EXISTS transaction_monitoring CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
运行这3行代码来创建用户并授权：
CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY 'apppass';
GRANT ALL PRIVILEGES ON transaction_monitoring.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;