/*
 Navicat Premium Dump SQL

 Source Server         : Jenrimark
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : LynxTrip

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 18/04/2026 16:08:36
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for address
-- ----------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `userid` bigint NOT NULL COMMENT '用户id',
  `address` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '地址',
  `name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '收货人',
  `phone` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '电话',
  `isdefault` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '是否默认地址[是/否]',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1746166637825 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='地址';

-- ----------------------------
-- Records of address
-- ----------------------------
BEGIN;
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (1, '2021-03-03 15:14:31', 1, '宇宙银河系金星1号', '金某', '13823888881', '是');
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (2, '2021-03-03 15:14:31', 2, '宇宙银河系木星1号', '木某', '13823888882', '是');
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (3, '2021-03-03 15:14:31', 3, '宇宙银河系水星1号', '水某', '13823888883', '是');
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (4, '2021-03-03 15:14:31', 4, '宇宙银河系火星1号', '火某', '13823888884', '是');
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (5, '2021-03-03 15:14:31', 5, '宇宙银河系土星1号', '土某', '13823888885', '是');
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (6, '2021-03-03 15:14:31', 6, '宇宙银河系月球1号', '月某', '13823888886', '是');
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (1614757033717, '2021-03-03 15:37:13', 1614756982123, '广东省揭阳市榕城区中山街道北环城路16号中兴园', '陈一', '12312312312', '是');
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (1614757746498, '2021-03-03 15:49:05', 1614757696160, '广东省揭阳市榕城区中山街道Show艺数字油画中兴园', '陈一', '12312312312', '是');
INSERT INTO `address` (`id`, `addtime`, `userid`, `address`, `name`, `phone`, `isdefault`) VALUES (1746166637824, '2025-05-02 14:17:17', 1746166399778, '中国地质大学（武汉）', 'Jenrimark', '18471609769', '是');
COMMIT;

-- ----------------------------
-- Table structure for cart
-- ----------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tablename` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'zuixinxianlu' COMMENT '商品表名',
  `userid` bigint NOT NULL COMMENT '用户id',
  `goodid` bigint NOT NULL COMMENT '商品id',
  `goodname` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '商品名称',
  `picture` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '图片',
  `buynumber` int NOT NULL COMMENT '购买数量',
  `price` float DEFAULT NULL COMMENT '单价',
  `discountprice` float DEFAULT NULL COMMENT '会员价',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1772961157607 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='购物车表';

-- ----------------------------
-- Records of cart
-- ----------------------------
BEGIN;
INSERT INTO `cart` (`id`, `addtime`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`) VALUES (1614757109452, '2021-03-03 15:38:28', 'lvyouxianlu', 1614756982123, 1614756810110, '云南大理丽江6天5晚', 'http://localhost:8080/springbootmt74k/upload/1614756714294.jpg', 1, 2000, 0);
INSERT INTO `cart` (`id`, `addtime`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`) VALUES (1614758018421, '2021-03-03 15:53:37', 'zuixinxianlu', 1614757696160, 1614757619035, '桂林4天3晚', 'http://localhost:8080/springbootmt74k/upload/1614757585872.png', 1, 1200, 0);
INSERT INTO `cart` (`id`, `addtime`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`) VALUES (1703725765441, '2023-12-28 09:09:24', 'zuixinxianlu', 11, 42, '线路名称2', 'http://localhost:8080/springbootmt74k/upload/zuixinxianlu_fengmiantu2.jpg', 1, 99.9, 0);
INSERT INTO `cart` (`id`, `addtime`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`) VALUES (1703725987977, '2023-12-28 09:13:07', 'lvyouxianlu', 11, 35, '线路名称5', 'http://localhost:8080/springbootmt74k/upload/1614756215284.jpg', 1, 99.9, 0);
COMMIT;

-- ----------------------------
-- Table structure for chat
-- ----------------------------
DROP TABLE IF EXISTS `chat`;
CREATE TABLE `chat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `userid` bigint NOT NULL COMMENT '用户id',
  `adminid` bigint DEFAULT NULL COMMENT '管理员id',
  `ask` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '提问',
  `reply` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '回复',
  `isreply` int DEFAULT NULL COMMENT '是否回复',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1747734868837 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='客服聊天表';

-- ----------------------------
-- Records of chat
-- ----------------------------
BEGIN;
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (51, '2021-03-03 07:14:31', 1, 1, '提问1', '回复1', 1);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (52, '2021-03-03 07:14:31', 2, 2, '提问2', '回复2', 2);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (53, '2021-03-03 07:14:31', 3, 3, '提问3', '回复3', 3);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (54, '2021-03-03 07:14:31', 4, 4, '提问4', '回复4', 4);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (55, '2021-03-03 07:14:31', 5, 5, '提问5', '回复5', 5);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (56, '2021-03-03 07:14:31', 6, 6, '提问6', '回复6', 6);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (1614757183618, '2021-03-03 07:39:43', 1614756982123, NULL, '请问有国外旅游线路吗', NULL, 0);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (1614757346257, '2021-03-03 07:42:26', 1614756982123, 1, NULL, '1', NULL);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (1614757887672, '2021-03-03 07:51:26', 1614757696160, NULL, '请问有国外的旅游线路吗', NULL, 0);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (1614757928392, '2021-03-03 07:52:07', 1614757696160, 1, NULL, '暂时没有', NULL);
INSERT INTO `chat` (`id`, `addtime`, `userid`, `adminid`, `ask`, `reply`, `isreply`) VALUES (1747734868836, '2025-05-20 17:54:28', 1746166399778, NULL, '1', NULL, 1);
COMMIT;

-- ----------------------------
-- Table structure for config
-- ----------------------------
DROP TABLE IF EXISTS `config`;
CREATE TABLE `config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '配置参数名称',
  `value` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '配置参数值',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='配置文件';

-- ----------------------------
-- Records of config
-- ----------------------------
BEGIN;
INSERT INTO `config` (`id`, `name`, `value`) VALUES (1, 'picture1', 'http://localhost:8080/springbootmt74k/upload/string1.jpg');
INSERT INTO `config` (`id`, `name`, `value`) VALUES (2, 'picture2', 'http://localhost:8080/springbootmt74k/upload/string2.jpg');
INSERT INTO `config` (`id`, `name`, `value`) VALUES (3, 'picture3', 'http://localhost:8080/springbootmt74k/upload/string3.jpg');
INSERT INTO `config` (`id`, `name`, `value`) VALUES (4, 'picture4', 'http://localhost:8080/springbootmt74k/upload/string4.jpg');
COMMIT;

-- ----------------------------
-- Table structure for discusslvyouxianlu
-- ----------------------------
DROP TABLE IF EXISTS `discusslvyouxianlu`;
CREATE TABLE `discusslvyouxianlu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `refid` bigint NOT NULL COMMENT '关联表id',
  `userid` bigint NOT NULL COMMENT '用户id',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '评论内容',
  `reply` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '回复内容',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1747414385700 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='旅游线路评论表';

-- ----------------------------
-- Records of discusslvyouxianlu
-- ----------------------------
BEGIN;
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (111, '2021-03-03 15:14:31', 1, 1, '评论内容1', '回复内容1');
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (112, '2021-03-03 15:14:31', 2, 2, '评论内容2', '回复内容2');
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (113, '2021-03-03 15:14:31', 3, 3, '评论内容3', '回复内容3');
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (114, '2021-03-03 15:14:31', 4, 4, '评论内容4', '回复内容4');
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (115, '2021-03-03 15:14:31', 5, 5, '评论内容5', '回复内容5');
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (116, '2021-03-03 15:14:31', 6, 6, '评论内容6', '回复内容6');
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (1614757117444, '2021-03-03 15:38:37', 1614756810110, 1614756982123, '不错的路线', NULL);
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (1747117383378, '2025-05-13 14:23:02', 31, 1746166399778, '真不错', NULL);
INSERT INTO `discusslvyouxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (1747414385699, '2025-05-17 00:53:04', 1747413672433, 1746166399778, '沉浸在文化的熏陶，特别棒！', NULL);
COMMIT;

-- ----------------------------
-- Table structure for discusszuixinxianlu
-- ----------------------------
DROP TABLE IF EXISTS `discusszuixinxianlu`;
CREATE TABLE `discusszuixinxianlu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `refid` bigint NOT NULL COMMENT '关联表id',
  `userid` bigint NOT NULL COMMENT '用户id',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '评论内容',
  `reply` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '回复内容',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1614757866075 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='最新线路评论表';

-- ----------------------------
-- Records of discusszuixinxianlu
-- ----------------------------
BEGIN;
INSERT INTO `discusszuixinxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (121, '2021-03-03 15:14:31', 1, 1, '评论内容1', '回复内容1');
INSERT INTO `discusszuixinxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (122, '2021-03-03 15:14:31', 2, 2, '评论内容2', '回复内容2');
INSERT INTO `discusszuixinxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (123, '2021-03-03 15:14:31', 3, 3, '评论内容3', '回复内容3');
INSERT INTO `discusszuixinxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (124, '2021-03-03 15:14:31', 4, 4, '评论内容4', '回复内容4');
INSERT INTO `discusszuixinxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (125, '2021-03-03 15:14:31', 5, 5, '评论内容5', '回复内容5');
INSERT INTO `discusszuixinxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (126, '2021-03-03 15:14:31', 6, 6, '评论内容6', '回复内容6');
INSERT INTO `discusszuixinxianlu` (`id`, `addtime`, `refid`, `userid`, `content`, `reply`) VALUES (1614757866074, '2021-03-03 15:51:05', 1614757619035, 1614757696160, '不错的旅游线路', NULL);
COMMIT;

-- ----------------------------
-- Table structure for lvyouxianlu
-- ----------------------------
DROP TABLE IF EXISTS `lvyouxianlu`;
CREATE TABLE `lvyouxianlu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `xianlumingcheng` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '线路名称',
  `xianlufenlei` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '线路分类',
  `fengmiantu` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '封面图',
  `jingdianmingcheng` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '景点名称',
  `chufadi` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '出发地',
  `mudedi` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '目的地',
  `jiaotongfangshi` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '交通方式',
  `chuxingshijian` datetime DEFAULT NULL COMMENT '出行时间',
  `feiyongbaohan` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '费用包含',
  `xingchengluxian` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '行程路线',
  `clicktime` datetime DEFAULT NULL COMMENT '最近点击时间',
  `clicknum` int DEFAULT '0' COMMENT '点击次数',
  `price` float NOT NULL COMMENT '价格',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1747413672434 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='旅游线路';

-- ----------------------------
-- Records of lvyouxianlu
-- ----------------------------
BEGIN;
INSERT INTO `lvyouxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `clicktime`, `clicknum`, `price`) VALUES (31, '2021-03-03 15:14:31', '\"红土绿韵\"大别山精神传承三日游', '度假旅游', 'http://localhost:8080/springbootmt74k/upload/1747414054492.png', '景点名称1', '出发地1', '目的地1', '大巴', '2025-05-17 00:46:45', '费用包含1', '<p>行程路线1</p>', '2026-03-08 17:12:51', 24, 999);
INSERT INTO `lvyouxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `clicktime`, `clicknum`, `price`) VALUES (32, '2021-03-03 15:14:31', '宜昌\"三峡移民精神+巴楚文化\"四日游', '文化旅游', 'http://localhost:8080/springbootmt74k/upload/1614756156025.jpg', '景点名称2', '出发地2', '目的地2', '大巴', '2021-03-03 15:14:31', '费用包含2', '<p>行程路线2</p>', '2026-03-08 17:12:29', 13, 688);
INSERT INTO `lvyouxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `clicktime`, `clicknum`, `price`) VALUES (33, '2021-03-03 15:14:31', '武汉周边\"木兰红色教育+云雾山赏花\"一日游', '文化旅游', 'http://localhost:8080/springbootmt74k/upload/1747414188326.png', '景点名称3', '出发地3', '目的地3', '大巴', '2021-03-03 15:14:31', '费用包含3', '<p>行程路线3</p>', '2025-05-17 18:26:45', 6, 479);
INSERT INTO `lvyouxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `clicktime`, `clicknum`, `price`) VALUES (34, '2021-03-03 15:14:31', '孝感\"金秋银杏+红色记忆\"周末游', '短程旅游', 'http://localhost:8080/springbootmt74k/upload/1747414227257.png', '景点名称4', '出发地4', '目的地4', '大巴', '2021-03-03 15:14:31', '费用包含4', '<p>行程路线4</p>', '2025-05-17 00:50:09', 6, 888);
INSERT INTO `lvyouxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `clicktime`, `clicknum`, `price`) VALUES (35, '2021-03-03 15:14:31', '\"湘鄂西星火\"恩施土家风情三日游', '观光旅游', 'http://localhost:8080/springbootmt74k/upload/1747414256476.png', '景点名称5', '出发地5', '目的地5', '大巴', '2021-03-03 15:14:31', '费用包含5', '<p>行程路线5</p>', '2025-05-17 00:50:40', 23, 566);
INSERT INTO `lvyouxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `clicktime`, `clicknum`, `price`) VALUES (36, '2021-03-03 15:14:31', '\"汉江星火\"襄阳乡村振兴三日游', '文化旅游', 'http://localhost:8080/springbootmt74k/upload/1747414294444.png', '景点名称6', '出发地6', '目的地6', '大巴', '2021-03-03 15:14:31', '费用包含6', '<p>行程路线6</p>', '2025-05-17 00:51:13', 14, 788);
INSERT INTO `lvyouxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `clicktime`, `clicknum`, `price`) VALUES (1747413672433, '2025-05-17 00:41:11', '问道武当·田园拾趣·红色记忆三日游', '文化旅游', 'http://localhost:8080/springbootmt74k/upload/1747413668046.png', '武当山风景区（5A级）-竹山县圣水湖柑橘采摘园-郧阳革命烈士陵园-青龙山恐龙蛋化石群地质公园', '武汉', '十堰市', '高铁', '2025-06-01 00:00:00', '武汉-十堰往返高铁二等座\n2晚特色住宿（武当山民宿+竹山农家乐）\n行程所列景点首道门票\n当地旅游大巴全程接送\n2早餐+3正餐（含农家特色餐）\n专业导游服务\n旅游意外险', '<p>Day1：武汉-十堰-武当山</p><p>15:14 武汉站乘坐高铁前往十堰</p><p>17:30 抵达十堰，专车接往武当山</p><p>18:30 入住武当山特色民宿</p><p>19:00 晚餐（道家养生宴）</p><p>&nbsp;</p><p>Day2：武当山-竹山县</p><p>08:00 游览武当山（紫霄宫、太子坡、金顶）</p><p>12:00 景区内午餐</p><p>14:00 乘车前往竹山县</p><p>15:30 圣水湖柑橘采摘体验</p><p>18:00 入住农家乐，晚餐品尝农家菜</p><p>19:30 民歌欣赏+糍粑制作体验</p><p>&nbsp;</p><p>Day3：竹山县-郧阳区-武汉</p><p>08:00 前往郧阳革命烈士陵园</p><p>10:00 参观青龙山恐龙蛋化石群</p><p>12:00 午餐（当地特色餐）</p><p>14:00 选购特产（武当道茶、房县黄酒）</p><p>15:00 送站返回武汉</p><p>17:30 抵达武汉站，行程结束</p><p><br></p><p>温馨提示：</p><p>武当山海拔较高，建议携带保暖衣物</p><p>柑橘采摘为季节性项目（9-11月最佳）</p><p>本线路适合6-65岁身体健康人士参加</p><p><br></p><p>特别说明：</p><p>儿童价：1.2米以下680元（不占床、不含高铁票）</p><p>持老年证、学生证可优惠80元/人</p><p>10人以上团队可享专属优惠</p><p>预订方式：</p><p>十堰文旅集团官方热线：0719-8888XXXX</p><p>在线预订：www.shiyan3daytour.com</p>', '2025-05-17 00:55:12', 6, 1280);
COMMIT;

-- ----------------------------
-- Table structure for news
-- ----------------------------
DROP TABLE IF EXISTS `news`;
CREATE TABLE `news` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `title` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '标题',
  `introduction` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '简介',
  `picture` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '图片',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '内容',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1747412966231 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='旅游资讯';

-- ----------------------------
-- Records of news
-- ----------------------------
BEGIN;
INSERT INTO `news` (`id`, `addtime`, `title`, `introduction`, `picture`, `content`) VALUES (101, '2021-03-03 15:14:31', '将军故里·诗意田园——黄冈市红安县七里坪镇', '红安县是\"中国第一将军县\"，七里坪镇保存完好的明清老街与300多处革命遗址交织。春有万亩油菜花海，秋有稻浪翻滚，长胜街上的青砖黛瓦间藏着20余处苏维埃政权旧址。', 'http://localhost:8080/springbootmt74k/upload/1747403442749.jpg', '<p>		在湖北省红安县七里坪镇的长胜街上，有一家传统的铁匠铺，主人张铁山打铁已有四十年。过去，他的工作主要是为当地居民打造日常农具和厨刀，生活勉强维持。随着乡村振兴战略的推进，特别是红安县2019年启动的“红色记忆+”项目，这个历史悠久的古镇获得了新生，张铁山的铁匠铺也因此迎来了转机。</p><p>		七里坪作为革命老区，拥有丰富的红色文化遗产，但这些资源长期未得到有效开发。2019年后，当地政府与文旅专家合作，决定将镇上的古建筑及革命遗址进行旅游开发，并认为像张铁山这样的传统手艺人是宝贵的旅游资源。于是，一个创新的想法诞生了：将铁匠铺改造成革命兵器体验馆。改造后的铁匠铺新增了几个区域：展览区展示了红军时期的武器复制品，并附有历史故事；体验区允许游客亲手制作小型纪念品，如缩小版的“红军刀片”。</p><p>这一变化不仅让张铁山的月收入大幅增长，从2000元升至8000元以上，还吸引了他原本在武汉工作的儿子回乡帮忙管理线上销售。更重要的是，这种模式激发了其他传统手艺人的灵感，促进了本地文化产业的发展。</p><p>		总之，通过结合红色文化、传统手艺与现代旅游业，七里坪镇成功地实现了文化和经济的双重振兴，展现了历史与现代融合的魅力。</p><p><img src=\"http://localhost:8080/springbootmt74k/upload/1747412433347.png\"></p><p><img src=\"http://localhost:8080/springbootmt74k/upload/1747412457966.jpg\"></p>');
INSERT INTO `news` (`id`, `addtime`, `title`, `introduction`, `picture`, `content`) VALUES (102, '2021-03-03 15:14:31', '云上花田·胜利丰碑——恩施州咸丰县忠堡大捷遗址', '海拔1200米的忠堡镇拥有万亩高山杜鹃花海，核心区内的忠堡大捷纪念碑矗立在花丛中。游客可体验\"穿红军装走胜利路\"，在土家族吊脚楼里听老人讲述贺龙元帅的战场故事。', 'http://localhost:8080/springbootmt74k/upload/1747411636936.jpg', '<p><strong> 咸丰县忠堡镇依托红色旅游资源，创新打造\"战地花海\"生态农场项目，通过发展高山杜鹃种植和\"红色花蜜\"产业，实现年产值超千万元，带动村民年分红超80万元，获评\"湖北省乡村振兴创新案例\"。</strong></p><p><br></p><p>该项目主要包含三大特色产业：</p><p>&nbsp;高山杜鹃苗圃：培育\"革命红\"等特色品种，年育苗50万株，年销售额达300万元；</p><p>&nbsp;红色花蜜产业：开发\"胜利蜜\"等系列产品，年销售额突破500万元；</p><p>&nbsp;农旅融合项目：建设200亩杜鹃花海景观，配套红色教育基地。</p><p><br></p><p>项目运营采取\"村集体+合作社+农户\"模式：</p><p>提供固定就业岗位60余个</p><p>&nbsp;季节性用工200余人</p><p>带动132户农户增收</p><p>&nbsp;村集体年增收82万元</p><p><br></p><p>2022年，该项目凭借\"红色文化引领、特色产业支撑、农旅融合发展\"的创新模式，成功入选\"湖北省乡村振兴创新案例\"。目前，当地正规划建设杜鹃花精深加工厂，进一步延伸产业链条，提升产品附加值</p><p><img src=\"http://localhost:8080/springbootmt74k/upload/1747412683903.jpg\"></p>');
INSERT INTO `news` (`id`, `addtime`, `title`, `introduction`, `picture`, `content`) VALUES (103, '2021-03-03 15:14:31', '红叶映初心·古村焕新颜——孝感市大悟县金岭村', '这个600年历史的颜回后裔古村落，现存12处新四军医院旧址。秋季千亩乌桕红叶似火，夯土墙民宿与3D稻田画相映成趣，可参与\"纺线纳鞋底\"等支前劳动体验。', 'http://localhost:8080/springbootmt74k/upload/1747411743423.png', '<p>在湖北省大悟县，金岭村通过\"古村修复+红色研学\"的发展模式，走出了一条独具特色的乡村振兴之路。这个曾经负债累累的传统村落，如今集体经济年收入已突破百万元，成为当地乡村振兴的典范。</p><p>金岭村依托保存完好的明清古民居和丰富的红色文化资源，对村内12栋古建筑进行了保护性修缮，修复了新四军医院旧址等5处红色遗址。在保留原始村落格局和建筑风貌的基础上，村里开发了\"重走红军路\"等研学路线，开设了\"纺线纳鞋底\"等特色体验课程，每年吸引研学团队200余批次。</p><p>为丰富旅游业态，村民们创新推出\"红军粗粮宴\"系列菜品，研发了8种红色主题特色小吃，日均接待游客达200人次。在运营模式上，23户村民以老宅入股旅游公司，实行\"保底收益+按股分红\"的分配方式，既保障了村民利益，又调动了参与积极性。</p><p>通过系统培训，村里培养了15名村民讲解员和8名传统技艺传承人，还成功吸引12名青年返乡创业。如今，金岭村集体经济年收入达126万元，村民户均年增收3.2万元，带动就业83人。这一成功实践不仅让村子获评\"中国传统村落\"和\"湖北省乡村振兴示范村\"，还获得了央视《记住乡愁》栏目的专题报道。</p><p>展望未来，金岭村计划建设红色文创产业园，开发数字文旅产品，并积极申报国家3A级旅游景区，继续书写乡村振兴的新篇章。</p><p class=\"ql-align-center\">&nbsp;</p>');
INSERT INTO `news` (`id`, `addtime`, `title`, `introduction`, `picture`, `content`) VALUES (104, '2021-03-03 15:14:31', '茶山竹海里的慢生活——荆门市京山潼泉湖旅游区', '3000亩生态茶园与万亩竹林构成天然氧吧，游客可体验采茶制茶、竹林挖笋。特色星空木屋民宿采用当地石材建造，夜晚可观赏萤火虫生态景观。', 'http://localhost:8080/springbootmt74k/upload/1747411981899.png', '<p>在京山市潼泉湖旅游区，3000亩生态茶园与万亩竹林交相辉映，勾勒出一幅产业兴、生态美、百姓富的乡村振兴新图景。这片茶山竹海不仅成为都市人向往的\"慢生活\"胜地，更通过创新业态带动周边村民走上致富路。</p><p>依托得天独厚的自然资源，当地探索出\"茶旅融合+生态康养\"的发展模式。昔日的茶农们经过专业培训，转型为民宿管家、茶艺师和生态导览员。村民李大姐告诉记者：\"以前采茶年收入不到2万元，现在当民宿管家月薪就有3000多元，还能拿年终分红。\"</p><p>创新产品开发为产业注入新活力。旅游区推出的\"竹筒茶\"系列产品，将优质茶叶贮存在新鲜竹筒中自然醇化，既保留了传统工艺又增添了竹香特色，单件售价达198元仍供不应求。配套开发的竹编茶具、茶香竹炭等20余种衍生产品，年销售额突破800万元。</p><p>通过\"公司+合作社+农户\"的运营机制，周边5个村联合成立了产业联盟。2023年统计显示，联盟成员户均增收2.4万元，村集体平均增收15万元。旅游区还设立了乡村振兴专项基金，已累计投入120万元用于基础设施改造和技能培训。</p><p>夜幕降临，22栋星空木屋民宿亮起暖黄的灯光。这些采用当地石材和竹材建造的特色民宿，完美融入自然景观，夏季萤火虫观测项目更成为网红打卡点。随着二期工程的推进，预计2024年可新增就业岗位50个，带动户均年收入突破3万元。</p><p>潼泉湖的实践印证了\"绿水青山就是金山银山\"的发展理念。如今，这片茶山竹海正书写着乡村振兴的更美篇章，让更多村民在家门口吃上\"生态饭\"，共享发展成果。</p><p><img src=\"http://localhost:8080/springbootmt74k/upload/1747412742874.png\"></p><p><img src=\"http://localhost:8080/springbootmt74k/upload/1747412759677.png\"></p>');
INSERT INTO `news` (`id`, `addtime`, `title`, `introduction`, `picture`, `content`) VALUES (105, '2021-03-03 15:14:31', '悬崖上的桃花源——襄阳市保康县尧治河村', '这个海拔1600米的高山村落有\"云上药园\"之称，春季野生杜鹃满山，秋季层林尽染。保留着传统造纸作坊和夯土民居，可参与古法酿酒、药膳制作。', 'http://localhost:8080/springbootmt74k/upload/1747412071727.png', '<p>在鄂西北海拔1600米的崇山峻岭间，保康县尧治河村完成了一场令人惊叹的转型。这个曾经依靠磷矿开采的深山村落，如今已成为闻名遐迩的\"云上桃花源\"，实现了从\"卖矿石\"到\"卖风景\"的华丽转身。</p><p>面对资源枯竭的困境，村民们果断转变发展思路。通过集体众筹，先后投入3000余万元建设悬壁栈道和地矿博物馆，将废弃矿坑改造为特色景点。村里完整保留了传统造纸作坊、古法酿酒坊等民俗文化场所，开发出药膳制作、高山采药等体验项目。</p><p>转型带来实实在在的收益。2023年，全村接待游客15万人次，旅游综合收入突破2000万元，村民人均年收入达3.8万元。凭借\"矿山变景区、矿工变导游\"的创新实践，尧治河村先后荣获\"中国十大幸福村庄\"、\"全国乡村旅游重点村\"等称号。</p><p>如今的尧治河，春赏杜鹃、夏避酷暑、秋观红叶、冬玩冰雪，四季皆景的生态画卷，正书写着乡村振兴的生动篇章。</p><p><img src=\"http://localhost:8080/springbootmt74k/upload/1747412815005.png\"></p><p><img src=\"http://localhost:8080/springbootmt74k/upload/1747412830637.png\"></p>');
INSERT INTO `news` (`id`, `addtime`, `title`, `introduction`, `picture`, `content`) VALUES (106, '2021-03-03 15:14:31', '云端上的土家秘境——宜昌市五峰县栗子坪村', '在武陵山腹地的千米海拔之上，栗子坪村宛如一幅立体山水画卷。300余栋保存完好的土家吊脚楼群依山而建，层层叠叠的梯田随四季变换色彩——春日水光潋滟如镜，秋时稻浪翻滚似金。村中千年银杏树下，土家老人仍在用古法织造西兰卡普，非遗技艺与云海雾凇共同守望着这个\"中国最美休闲乡村\"的慢时光。', 'http://localhost:8080/springbootmt74k/upload/1747412167131.png', '<p>五峰县栗子坪村依托独特的土家文化和高山生态资源，走出了一条\"非遗传承+生态旅游\"的乡村振兴之路。这个藏在云端的传统村落，正通过文化赋能焕发新生机。</p><p>村里成立了土家织锦合作社，将古老的西兰卡普技艺转化为时尚文创产品。\"现在织一条围巾能卖800元，比我过去种一年土豆挣得还多。\"非遗传承人向阿婆笑着说。合作社开发的现代版西兰卡普手包、茶席等产品，通过电商平台远销海外，年销售额突破300万元。</p><p>通过\"传统民居保护性开发\"模式，村里将闲置吊脚楼改造成精品民宿。村民以房屋入股，由旅游公司统一运营，享受保底分红。2023年，民宿集群接待游客5万人次，带动户均增收2.8万元。村里还复原了古法榨油坊、水磨坊等传统生产场景，让游客体验原生态的土家生活。</p><p><img src=\"http://localhost:8080/springbootmt74k/upload/1747412855255.png\"></p>');
INSERT INTO `news` (`id`, `addtime`, `title`, `introduction`, `picture`, `content`) VALUES (1747412966230, '2025-05-17 00:29:26', '大别山下的田园诗——黄冈市罗田县圣人堂村', '圣人堂村坐落在天堂寨风景区腹地，以\"红叶第一村\"闻名遐迩。每年深秋，乌桕树将山谷染成赭红，与白墙黛瓦的徽派民居构成绝美画卷。村民沿袭着\"耕读传家\"的传统，稻田养鱼、板栗林下种天麻的复合农业模式，让这里的田园既具诗意又富生机。清晨的薄雾中，牧牛人穿过百年石拱桥的身影，定格成大别山区最动人的乡愁记忆。', 'http://localhost:8080/springbootmt74k/upload/1747412964601.png', '<p>	罗田县圣人堂村通过\"农旅融合+文化IP\"的发展模式，让传统农耕焕发新活力。这个曾因人口外流而萧条的村落，如今成为远近闻名的网红打卡地。</p><p>	村里创新推出\"稻田剧场\"，在金色稻浪中实景演出农耕文化剧，场场爆满。\"演出用的镰刀、蓑衣都是真家伙，演员全是本村农民。\"村支书介绍道。这种沉浸式体验项目，带动周边农家乐日均接待游客200余人次。</p><p>	通过发展立体农业，村民在板栗林下套种天麻、茯苓等中药材，亩产效益提升3倍。村里还注册了\"圣人堂红叶\"地理标志，开发红叶标本、红叶拓印画等文创产品，年销售额达150万元。2023年，村民人均可支配收入达2.6万元，较五年前翻了一番。</p><p><br></p>');
COMMIT;

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `orderid` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '订单编号',
  `tablename` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'zuixinxianlu' COMMENT '商品表名',
  `userid` bigint NOT NULL COMMENT '用户id',
  `goodid` bigint NOT NULL COMMENT '商品id',
  `goodname` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '商品名称',
  `picture` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '商品图片',
  `buynumber` int NOT NULL COMMENT '购买数量',
  `price` float NOT NULL DEFAULT '0' COMMENT '价格/积分',
  `discountprice` float DEFAULT '0' COMMENT '折扣价格',
  `total` float NOT NULL DEFAULT '0' COMMENT '总价格/总积分',
  `discounttotal` float DEFAULT '0' COMMENT '折扣总价格',
  `type` int DEFAULT '1' COMMENT '支付类型',
  `status` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '状态',
  `address` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '地址',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `orderid` (`orderid`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1772961172288 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='订单';

-- ----------------------------
-- Records of orders
-- ----------------------------
BEGIN;
INSERT INTO `orders` (`id`, `addtime`, `orderid`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`, `total`, `discounttotal`, `type`, `status`, `address`) VALUES (1614757238256, '2021-03-03 15:40:37', '20213315403614711707', 'lvyouxianlu', 1614756982123, 1614756810110, '云南大理丽江6天5晚', 'http://localhost:8080/springbootmt74k/upload/1614756714294.jpg', 1, 2000, 2000, 3000, 2000, 1, '未支付', '广东省揭阳市榕城区中山街道北环城路16号中兴园');
INSERT INTO `orders` (`id`, `addtime`, `orderid`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`, `total`, `discounttotal`, `type`, `status`, `address`) VALUES (1614757895454, '2021-03-03 15:51:34', '20213315513353374216', 'lvyouxianlu', 1614757696160, 1614757564118, '云南6天5晚', 'http://localhost:8080/springbootmt74k/upload/1614757486132.jpg', 1, 2500, 2500, 2500, 2500, 1, '已完成', '广东省揭阳市榕城区中山街道Show艺数字油画中兴园');
INSERT INTO `orders` (`id`, `addtime`, `orderid`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`, `total`, `discounttotal`, `type`, `status`, `address`) VALUES (1614758027300, '2021-03-03 15:53:46', '20213315534553649075', 'zuixinxianlu', 1614757696160, 1614757619035, '桂林4天3晚', 'http://localhost:8080/springbootmt74k/upload/1614757585872.png', 1, 1200, 1200, 1200, 1200, 1, '已支付', '广东省揭阳市榕城区中山街道Show艺数字油画中兴园');
INSERT INTO `orders` (`id`, `addtime`, `orderid`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`, `total`, `discounttotal`, `type`, `status`, `address`) VALUES (1746166662374, '2025-05-02 14:17:41', '20255214174140553219', 'lvyouxianlu', 1746166399778, 35, '线路名称5', 'http://localhost:8080/springbootmt74k/upload/1614756215284.jpg', 1, 99.9, 99.9, 99.9, 99.9, 1, '已退款', '中国地大');
INSERT INTO `orders` (`id`, `addtime`, `orderid`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`, `total`, `discounttotal`, `type`, `status`, `address`) VALUES (1746338678803, '2025-05-04 14:04:38', '2025541443816966460', 'lvyouxianlu', 1746166399778, 31, '线路名称1', 'http://localhost:8080/springbootmt74k/upload/lvyouxianlu_fengmiantu1.jpg', 1, 99.9, 99.9, 99.9, 99.9, 1, '已支付', '中国地大');
INSERT INTO `orders` (`id`, `addtime`, `orderid`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`, `total`, `discounttotal`, `type`, `status`, `address`) VALUES (1746338748760, '2025-05-04 14:05:48', '2025541454873404054', 'lvyouxianlu', 1746166399778, 32, '线路名称2', 'http://localhost:8080/springbootmt74k/upload/1614756156025.jpg', 1, 99.9, 99.9, 99.9, 99.9, 1, '已支付', '中国地大');
INSERT INTO `orders` (`id`, `addtime`, `orderid`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`, `total`, `discounttotal`, `type`, `status`, `address`) VALUES (1772961171936, '2026-03-08 17:12:51', '20263817125163378333', 'zuixinxianlu', 1746166399778, 42, '线路名称2', 'http://localhost:8080/springbootmt74k/upload/zuixinxianlu_fengmiantu2.jpg', 1, 99.9, 99.9, 199.8, 99.9, 1, '已支付', '中国地质大学（武汉）');
INSERT INTO `orders` (`id`, `addtime`, `orderid`, `tablename`, `userid`, `goodid`, `goodname`, `picture`, `buynumber`, `price`, `discountprice`, `total`, `discounttotal`, `type`, `status`, `address`) VALUES (1772961172287, '2026-03-08 17:12:51', '20263817125163858423', 'lvyouxianlu', 1746166399778, 31, '线路名称1', 'http://localhost:8080/springbootmt74k/upload/lvyouxianlu_fengmiantu1.jpg', 1, 99.9, 99.9, 199.8, 99.9, 1, '已支付', '中国地质大学（武汉）');
COMMIT;

-- ----------------------------
-- Table structure for storeup
-- ----------------------------
DROP TABLE IF EXISTS `storeup`;
CREATE TABLE `storeup` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `userid` bigint NOT NULL COMMENT '用户id',
  `refid` bigint DEFAULT NULL COMMENT '收藏id',
  `tablename` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表名',
  `name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '收藏名称',
  `picture` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '收藏图片',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1747118922487 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='收藏表';

-- ----------------------------
-- Records of storeup
-- ----------------------------
BEGIN;
INSERT INTO `storeup` (`id`, `addtime`, `userid`, `refid`, `tablename`, `name`, `picture`) VALUES (1614757107467, '2021-03-03 15:38:26', 1614756982123, 1614756810110, 'lvyouxianlu', '云南大理丽江6天5晚', 'http://localhost:8080/springbootmt74k/upload/1614756714294.jpg');
INSERT INTO `storeup` (`id`, `addtime`, `userid`, `refid`, `tablename`, `name`, `picture`) VALUES (1614757140140, '2021-03-03 15:38:59', 1614756982123, 1614756888348, 'zuixinxianlu', '桂林阳朔4天3晚', 'http://localhost:8080/springbootmt74k/upload/1614756840971.png');
INSERT INTO `storeup` (`id`, `addtime`, `userid`, `refid`, `tablename`, `name`, `picture`) VALUES (1614757812948, '2021-03-03 15:50:12', 1614757696160, 1614757564118, 'lvyouxianlu', '云南6天5晚', 'http://localhost:8080/springbootmt74k/upload/1614757486132.jpg');
INSERT INTO `storeup` (`id`, `addtime`, `userid`, `refid`, `tablename`, `name`, `picture`) VALUES (1614757858660, '2021-03-03 15:50:57', 1614757696160, 1614757619035, 'zuixinxianlu', '桂林4天3晚', 'http://localhost:8080/springbootmt74k/upload/1614757585872.png');
INSERT INTO `storeup` (`id`, `addtime`, `userid`, `refid`, `tablename`, `name`, `picture`) VALUES (1703725994232, '2023-12-28 09:13:13', 11, 35, 'lvyouxianlu', '线路名称5', 'http://localhost:8080/springbootmt74k/upload/1614756215284.jpg');
INSERT INTO `storeup` (`id`, `addtime`, `userid`, `refid`, `tablename`, `name`, `picture`) VALUES (1747118922486, '2025-05-13 14:48:42', 1746166399778, 42, 'zuixinxianlu', '线路名称2', 'http://localhost:8080/springbootmt74k/upload/zuixinxianlu_fengmiantu2.jpg');
COMMIT;

-- ----------------------------
-- Table structure for token
-- ----------------------------
DROP TABLE IF EXISTS `token`;
CREATE TABLE `token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userid` bigint NOT NULL COMMENT '用户id',
  `username` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户名',
  `tablename` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表名',
  `role` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '角色',
  `token` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  `expiratedtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='token表';

-- ----------------------------
-- Records of token
-- ----------------------------
BEGIN;
INSERT INTO `token` (`id`, `userid`, `username`, `tablename`, `role`, `token`, `addtime`, `expiratedtime`) VALUES (1, 1, 'abo', 'users', '管理员', 'iaa5x16wx9ynjl3c9r1oku38t6zm6qh0', '2021-03-03 15:19:57', '2026-04-18 15:50:22');
INSERT INTO `token` (`id`, `userid`, `username`, `tablename`, `role`, `token`, `addtime`, `expiratedtime`) VALUES (2, 1614756982123, '1', 'yonghu', '用户', 'x1jatumvhfm9ra95gbc6d5538tt8niyt', '2021-03-03 15:36:27', '2021-03-03 08:36:28');
INSERT INTO `token` (`id`, `userid`, `username`, `tablename`, `role`, `token`, `addtime`, `expiratedtime`) VALUES (3, 1614757696160, '2', 'yonghu', '用户', 'l4cfc4soq7d1xvs5rd8emc3yt8acwilq', '2021-03-03 15:48:21', '2021-03-03 08:52:51');
INSERT INTO `token` (`id`, `userid`, `username`, `tablename`, `role`, `token`, `addtime`, `expiratedtime`) VALUES (4, 11, '用户1', 'yonghu', '用户', '8rv4mjx40xo688s6jbxs2zxlalvrbwgl', '2023-12-28 09:09:09', '2023-12-28 10:12:58');
INSERT INTO `token` (`id`, `userid`, `username`, `tablename`, `role`, `token`, `addtime`, `expiratedtime`) VALUES (5, 1746166399778, '123456', 'yonghu', '用户', 'nsh2z07we76itxagt8p8g83xblqka5c4', '2025-05-02 14:13:30', '2026-03-08 17:25:28');
INSERT INTO `token` (`id`, `userid`, `username`, `tablename`, `role`, `token`, `addtime`, `expiratedtime`) VALUES (6, 1776495538447, 'admin', 'yonghu', '用户', 'ibomybfp6bx156fgiw3g242udctidwyb', '2026-04-18 14:59:03', '2026-04-18 15:59:04');
COMMIT;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `role` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '管理员' COMMENT '角色',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '新增时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户表';

-- ----------------------------
-- Records of users
-- ----------------------------
BEGIN;
INSERT INTO `users` (`id`, `username`, `password`, `role`, `addtime`) VALUES (1, 'admin', '123456', '管理员', '2021-03-03 15:14:31');
COMMIT;

-- ----------------------------
-- Table structure for xianlufenlei
-- ----------------------------
DROP TABLE IF EXISTS `xianlufenlei`;
CREATE TABLE `xianlufenlei` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `xianlufenlei` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '线路分类',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `xianlufenlei` (`xianlufenlei`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1614757461166 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='线路分类';

-- ----------------------------
-- Records of xianlufenlei
-- ----------------------------
BEGIN;
INSERT INTO `xianlufenlei` (`id`, `addtime`, `xianlufenlei`) VALUES (21, '2025-05-17 15:14:31', '乡村风景');
INSERT INTO `xianlufenlei` (`id`, `addtime`, `xianlufenlei`) VALUES (22, '2025-05-17 15:14:31', '度假旅游');
INSERT INTO `xianlufenlei` (`id`, `addtime`, `xianlufenlei`) VALUES (23, '2025-05-17 15:14:31', '探险考察');
INSERT INTO `xianlufenlei` (`id`, `addtime`, `xianlufenlei`) VALUES (24, '2025-05-17 15:14:31', '文化底蕴');
INSERT INTO `xianlufenlei` (`id`, `addtime`, `xianlufenlei`) VALUES (25, '2025-05-17 15:14:31', '短程旅游');
INSERT INTO `xianlufenlei` (`id`, `addtime`, `xianlufenlei`) VALUES (26, '2025-05-17 15:44:20', '红色基地');
COMMIT;

-- ----------------------------
-- Table structure for yonghu
-- ----------------------------
DROP TABLE IF EXISTS `yonghu`;
CREATE TABLE `yonghu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `yonghuming` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户名',
  `mima` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `xingming` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '姓名',
  `touxiang` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '头像',
  `xingbie` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '性别',
  `lianxidianhua` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '联系电话',
  `money` float DEFAULT '0' COMMENT '余额',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `yonghuming` (`yonghuming`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1776495538448 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户';

-- ----------------------------
-- Records of yonghu
-- ----------------------------
BEGIN;
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (11, '2021-03-03 15:14:31', '用户1', '123456', '姓名1', 'http://localhost:8080/springbootmt74k/upload/yonghu_touxiang1.jpg', '男', '13823888881', 100);
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (12, '2021-03-03 15:14:31', '用户2', '123456', '姓名2', 'http://localhost:8080/springbootmt74k/upload/yonghu_touxiang2.jpg', '男', '13823888882', 100);
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (13, '2021-03-03 15:14:31', '用户3', '123456', '姓名3', 'http://localhost:8080/springbootmt74k/upload/yonghu_touxiang3.jpg', '男', '13823888883', 100);
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (14, '2021-03-03 15:14:31', '用户4', '123456', '姓名4', 'http://localhost:8080/springbootmt74k/upload/yonghu_touxiang4.jpg', '男', '13823888884', 100);
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (15, '2021-03-03 15:14:31', '用户5', '123456', '姓名5', 'http://localhost:8080/springbootmt74k/upload/yonghu_touxiang5.jpg', '男', '13823888885', 100);
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (16, '2021-03-03 15:14:31', '用户6', '123456', '姓名6', 'http://localhost:8080/springbootmt74k/upload/yonghu_touxiang6.jpg', '男', '13823888886', 100);
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (1614757696160, '2021-03-03 15:48:16', '2', '2', '陈一', 'http://localhost:8080/springbootmt74k/upload/1614757715279.jpeg', '女', '12312345678', 1700);
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (1746166399778, '2025-05-02 14:13:19', '123456', '123456', 'Jenrimark', 'http://localhost:8080/springbootmt74k/upload/1747117207072.jpg', '男', '16688489420', 9809.2);
INSERT INTO `yonghu` (`id`, `addtime`, `yonghuming`, `mima`, `xingming`, `touxiang`, `xingbie`, `lianxidianhua`, `money`) VALUES (1776495538447, '2026-04-18 14:58:58', 'admin', '123456', '123', NULL, NULL, '18471609769', 0);
COMMIT;

-- ----------------------------
-- Table structure for zuixinxianlu
-- ----------------------------
DROP TABLE IF EXISTS `zuixinxianlu`;
CREATE TABLE `zuixinxianlu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `addtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `xianlumingcheng` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '线路名称',
  `xianlufenlei` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '线路分类',
  `fengmiantu` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '封面图',
  `jingdianmingcheng` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '景点名称',
  `chufadi` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '出发地',
  `mudedi` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '目的地',
  `jiaotongfangshi` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '交通方式',
  `chuxingshijian` datetime DEFAULT NULL COMMENT '出行时间',
  `feiyongbaohan` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '费用包含',
  `xingchengluxian` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '行程路线',
  `price` float NOT NULL COMMENT '价格',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1614757619036 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='最新线路';

-- ----------------------------
-- Records of zuixinxianlu
-- ----------------------------
BEGIN;
INSERT INTO `zuixinxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `price`) VALUES (41, '2021-03-03 15:14:31', '线路名称1', '探险考察', 'http://localhost:8080/springbootmt74k/upload/zuixinxianlu_fengmiantu1.jpg', '景点名称1', '出发地1', '目的地1', '大巴', '2021-03-03 15:14:31', '费用包含1', '<p><img src=\"http://localhost:8080/springbootmt74k/upload/1614756033607.png\"></p>', 99.9);
INSERT INTO `zuixinxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `price`) VALUES (42, '2021-03-03 15:14:31', '线路名称2', '文化底蕴', 'http://localhost:8080/springbootmt74k/upload/zuixinxianlu_fengmiantu2.jpg', '景点名称2', '出发地2', '目的地2', '大巴', '2021-03-03 15:14:31', '费用包含2', '<p>行程路线2</p>', 99.9);
INSERT INTO `zuixinxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `price`) VALUES (43, '2021-03-03 15:14:31', '线路名称3', '度假旅游', 'http://localhost:8080/springbootmt74k/upload/1614756260482.jpg', '景点名称3', '出发地3', '目的地3', '大巴', '2021-03-03 15:14:31', '费用包含3', '<p>行程路线3</p>', 99.9);
INSERT INTO `zuixinxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `price`) VALUES (44, '2021-03-03 15:14:31', '线路名称4', '乡村风景', 'http://localhost:8080/springbootmt74k/upload/1614756298370.jpg', '景点名称4', '出发地4', '目的地4', '大巴', '2021-03-03 15:14:31', '费用包含4', '<p>行程路线4</p>', 99.9);
INSERT INTO `zuixinxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `price`) VALUES (45, '2021-03-03 15:14:31', '线路名称5', '短程旅游', 'http://localhost:8080/springbootmt74k/upload/1614756309699.jpeg', '景点名称5', '出发地5', '目的地5', '大巴', '2021-03-03 15:14:31', '费用包含5', '<p>行程路线5</p>', 99.9);
INSERT INTO `zuixinxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `price`) VALUES (46, '2021-03-03 15:14:31', '线路名称6', '探险考察', 'http://localhost:8080/springbootmt74k/upload/zuixinxianlu_fengmiantu6.jpg', '景点名称6', '出发地6', '目的地6', '大巴', '2021-03-03 15:14:31', '费用包含6', '<p>行程路线6</p>', 99.9);
INSERT INTO `zuixinxianlu` (`id`, `addtime`, `xianlumingcheng`, `xianlufenlei`, `fengmiantu`, `jingdianmingcheng`, `chufadi`, `mudedi`, `jiaotongfangshi`, `chuxingshijian`, `feiyongbaohan`, `xingchengluxian`, `price`) VALUES (1614757619035, '2021-03-03 15:46:58', '桂林4天3晚', '文化底蕴', 'http://localhost:8080/springbootmt74k/upload/1614757585872.png', '漓江', '梅州', '桂林', '高铁', '2021-03-03 07:46:40', '住宿费来回车费等', '<p><img src=\"http://localhost:8080/springbootmt74k/upload/1614757617098.png\"></p>', 1200);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
