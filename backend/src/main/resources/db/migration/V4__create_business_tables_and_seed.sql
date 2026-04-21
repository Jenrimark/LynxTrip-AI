CREATE TABLE IF NOT EXISTS xianlufenlei (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  xianlufenlei VARCHAR(64) NOT NULL,
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lvyouxianlu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  xianlumingcheng VARCHAR(255) NOT NULL,
  xianlufenlei VARCHAR(64),
  fengmiantu VARCHAR(255),
  jingdianmingcheng VARCHAR(255),
  chufadi VARCHAR(64),
  mudedi VARCHAR(64),
  jiaotongfangshi VARCHAR(32),
  chuxingshijian VARCHAR(64),
  feiyongbaohan TEXT,
  xingchengluxian LONGTEXT,
  clicktime VARCHAR(64),
  clicknum INT NOT NULL DEFAULT 0,
  price DECIMAL(12, 2) NOT NULL DEFAULT 0,
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS zuixinxianlu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  xianlumingcheng VARCHAR(255) NOT NULL,
  xianlufenlei VARCHAR(64),
  fengmiantu VARCHAR(255),
  jingdianmingcheng VARCHAR(255),
  chufadi VARCHAR(64),
  mudedi VARCHAR(64),
  jiaotongfangshi VARCHAR(32),
  chuxingshijian VARCHAR(64),
  feiyongbaohan TEXT,
  xingchengluxian LONGTEXT,
  clicktime VARCHAR(64),
  clicknum INT NOT NULL DEFAULT 0,
  price DECIMAL(12, 2) NOT NULL DEFAULT 0,
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS news (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  introduction TEXT,
  picture VARCHAR(255),
  content LONGTEXT,
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  userid BIGINT NOT NULL,
  tablename VARCHAR(64) NOT NULL,
  goodid BIGINT NOT NULL,
  goodname VARCHAR(255),
  picture VARCHAR(255),
  buynumber INT NOT NULL DEFAULT 1,
  price DECIMAL(12, 2) NOT NULL DEFAULT 0,
  discountprice DECIMAL(12, 2) NOT NULL DEFAULT 0,
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_cart_userid (userid)
);

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  userid BIGINT NOT NULL,
  orderid VARCHAR(64) NOT NULL,
  tablename VARCHAR(64),
  goodid BIGINT,
  goodname VARCHAR(255),
  picture VARCHAR(255),
  buynumber INT NOT NULL DEFAULT 1,
  price DECIMAL(12, 2) NOT NULL DEFAULT 0,
  discountprice DECIMAL(12, 2) NOT NULL DEFAULT 0,
  total DECIMAL(12, 2) NOT NULL DEFAULT 0,
  discounttotal DECIMAL(12, 2) NOT NULL DEFAULT 0,
  type INT NOT NULL DEFAULT 1,
  status VARCHAR(32) NOT NULL DEFAULT '未支付',
  address VARCHAR(255),
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_orders_userid (userid)
);

CREATE TABLE IF NOT EXISTS storeup (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  userid BIGINT NOT NULL,
  refid BIGINT NOT NULL,
  tablename VARCHAR(64) NOT NULL,
  name VARCHAR(255),
  picture VARCHAR(255),
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_storeup_user_ref (userid, tablename, refid)
);

CREATE TABLE IF NOT EXISTS chat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  userid BIGINT NOT NULL,
  adminid BIGINT,
  ask TEXT,
  reply TEXT,
  isreply INT NOT NULL DEFAULT 0,
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_chat_userid (userid)
);

CREATE TABLE IF NOT EXISTS gallery (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  userid BIGINT NOT NULL,
  title VARCHAR(255),
  photoUrl VARCHAR(255),
  note TEXT,
  takenAt VARCHAR(64),
  location VARCHAR(255),
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_gallery_userid (userid)
);

CREATE TABLE IF NOT EXISTS trips (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  userid BIGINT NOT NULL,
  title VARCHAR(255),
  payload LONGTEXT,
  addtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_trips_userid (userid)
);

INSERT INTO xianlufenlei(xianlufenlei)
SELECT '乡村风景' WHERE NOT EXISTS (SELECT 1 FROM xianlufenlei WHERE xianlufenlei='乡村风景');
INSERT INTO xianlufenlei(xianlufenlei)
SELECT '度假旅游' WHERE NOT EXISTS (SELECT 1 FROM xianlufenlei WHERE xianlufenlei='度假旅游');
INSERT INTO xianlufenlei(xianlufenlei)
SELECT '探险考察' WHERE NOT EXISTS (SELECT 1 FROM xianlufenlei WHERE xianlufenlei='探险考察');

INSERT INTO lvyouxianlu(
  xianlumingcheng,xianlufenlei,fengmiantu,jingdianmingcheng,chufadi,mudedi,jiaotongfangshi,chuxingshijian,feiyongbaohan,xingchengluxian,clicknum,price
)
SELECT
  '问道武当·田园拾趣三日游','文化旅游','', '武当山-圣水湖-郧阳', '武汉', '十堰市','高铁','2026-05-01',
  '往返交通、住宿、门票、导游','Day1 武当山；Day2 田园体验；Day3 人文返程',6,1280
WHERE NOT EXISTS (SELECT 1 FROM lvyouxianlu WHERE xianlumingcheng='问道武当·田园拾趣三日游');

INSERT INTO zuixinxianlu(
  xianlumingcheng,xianlufenlei,fengmiantu,jingdianmingcheng,chufadi,mudedi,jiaotongfangshi,chuxingshijian,feiyongbaohan,xingchengluxian,clicknum,price
)
SELECT
  '桂林4天3晚','文化底蕴','','漓江','梅州','桂林','高铁','2026-06-01',
  '住宿费来回车费等','桂林经典行程',3,1200
WHERE NOT EXISTS (SELECT 1 FROM zuixinxianlu WHERE xianlumingcheng='桂林4天3晚');

INSERT INTO news(title,introduction,picture,content)
SELECT
  '将军故里·诗意田园——黄冈市红安县七里坪镇',
  '红安县是中国第一将军县，红色遗址与田园风光交织。',
  '',
  '七里坪镇融合红色文化与乡村旅游，适合周末研学与亲子出游。'
WHERE NOT EXISTS (SELECT 1 FROM news WHERE title='将军故里·诗意田园——黄冈市红安县七里坪镇');
