/*
 Navicat Premium Data Transfer

 Source Server         : 10.20.152.211-区块链
 Source Server Type    : MySQL
 Source Server Version : 80019
 Source Host           : 10.20.152.211:3306
 Source Schema         : proguard

 Target Server Type    : MySQL
 Target Server Version : 80019
 File Encoding         : 65001

 Date: 13/01/2022 19:44:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account
-- ----------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `account` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `password` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `descr` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `dept_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of account
-- ----------------------------
INSERT INTO `account` VALUES (1, 'test', 'test', '123123', NULL, 1);
INSERT INTO `account` VALUES (2, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (3, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (4, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (5, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (6, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (7, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (8, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (9, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (10, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (11, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (12, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (13, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (14, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (15, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (16, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (17, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (18, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (19, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (20, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (21, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (22, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (23, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (24, '张三', '1', '123123', NULL, NULL);
INSERT INTO `account` VALUES (25, '张三', '1', '123123', NULL, NULL);

-- ----------------------------
-- Table structure for dept
-- ----------------------------
DROP TABLE IF EXISTS `dept`;
CREATE TABLE `dept`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `dept_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `dept_descr` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dept
-- ----------------------------
INSERT INTO `dept` VALUES (1, '安全管理', '部门');

-- ----------------------------
-- View structure for account_dept
-- ----------------------------
DROP VIEW IF EXISTS `account_dept`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `account_dept` AS select `a`.`id` AS `id`,`a`.`account` AS `account`,`a`.`descr` AS `descr`,`a`.`name` AS `name`,`a`.`password` AS `password`,`d`.`dept_name` AS `dept_name`,`d`.`dept_descr` AS `dept_descr` from (`account` `a` left join `dept` `d` on((`a`.`dept_id` = `d`.`id`)));

SET FOREIGN_KEY_CHECKS = 1;
