-- Standardize business column names to clear English naming.

ALTER TABLE users
  CHANGE COLUMN yonghuming username VARCHAR(64) NOT NULL,
  CHANGE COLUMN mima password VARCHAR(128) NOT NULL,
  CHANGE COLUMN xingming display_name VARCHAR(64) NULL,
  CHANGE COLUMN touxiang avatar_url VARCHAR(255) NULL,
  CHANGE COLUMN xingbie gender VARCHAR(16) NULL,
  CHANGE COLUMN lianxidianhua phone VARCHAR(32) NULL,
  CHANGE COLUMN money balance DECIMAL(12, 2) NOT NULL DEFAULT 0,
  CHANGE COLUMN shimingrenzheng identity_status VARCHAR(32) NOT NULL DEFAULT '未认证',
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE route_categories
  CHANGE COLUMN xianlufenlei name VARCHAR(64) NOT NULL,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE travel_routes
  CHANGE COLUMN xianlumingcheng name VARCHAR(255) NOT NULL,
  CHANGE COLUMN xianlufenlei category VARCHAR(64) NULL,
  CHANGE COLUMN fengmiantu cover_url VARCHAR(255) NULL,
  CHANGE COLUMN jingdianmingcheng attraction_name VARCHAR(255) NULL,
  CHANGE COLUMN chufadi departure VARCHAR(64) NULL,
  CHANGE COLUMN mudedi destination VARCHAR(64) NULL,
  CHANGE COLUMN jiaotongfangshi transport VARCHAR(32) NULL,
  CHANGE COLUMN chuxingshijian departure_time VARCHAR(64) NULL,
  CHANGE COLUMN feiyongbaohan cost_includes TEXT NULL,
  CHANGE COLUMN xingchengluxian itinerary LONGTEXT NULL,
  CHANGE COLUMN clicktime last_clicked_at VARCHAR(64) NULL,
  CHANGE COLUMN clicknum click_count INT NOT NULL DEFAULT 0,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE latest_routes
  CHANGE COLUMN xianlumingcheng name VARCHAR(255) NOT NULL,
  CHANGE COLUMN xianlufenlei category VARCHAR(64) NULL,
  CHANGE COLUMN fengmiantu cover_url VARCHAR(255) NULL,
  CHANGE COLUMN jingdianmingcheng attraction_name VARCHAR(255) NULL,
  CHANGE COLUMN chufadi departure VARCHAR(64) NULL,
  CHANGE COLUMN mudedi destination VARCHAR(64) NULL,
  CHANGE COLUMN jiaotongfangshi transport VARCHAR(32) NULL,
  CHANGE COLUMN chuxingshijian departure_time VARCHAR(64) NULL,
  CHANGE COLUMN feiyongbaohan cost_includes TEXT NULL,
  CHANGE COLUMN xingchengluxian itinerary LONGTEXT NULL,
  CHANGE COLUMN clicktime last_clicked_at VARCHAR(64) NULL,
  CHANGE COLUMN clicknum click_count INT NOT NULL DEFAULT 0,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE travel_news
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE shopping_cart
  CHANGE COLUMN userid user_id BIGINT NOT NULL,
  CHANGE COLUMN tablename table_name VARCHAR(64) NOT NULL,
  CHANGE COLUMN goodid product_id BIGINT NOT NULL,
  CHANGE COLUMN goodname product_name VARCHAR(255) NULL,
  CHANGE COLUMN buynumber buy_number INT NOT NULL DEFAULT 1,
  CHANGE COLUMN discountprice discount_price DECIMAL(12, 2) NOT NULL DEFAULT 0,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE purchase_orders
  CHANGE COLUMN userid user_id BIGINT NOT NULL,
  CHANGE COLUMN orderid order_no VARCHAR(64) NOT NULL,
  CHANGE COLUMN tablename table_name VARCHAR(64) NULL,
  CHANGE COLUMN goodid product_id BIGINT NULL,
  CHANGE COLUMN goodname product_name VARCHAR(255) NULL,
  CHANGE COLUMN buynumber buy_number INT NOT NULL DEFAULT 1,
  CHANGE COLUMN discountprice discount_price DECIMAL(12, 2) NOT NULL DEFAULT 0,
  CHANGE COLUMN discounttotal discount_total DECIMAL(12, 2) NOT NULL DEFAULT 0,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE favorites
  CHANGE COLUMN userid user_id BIGINT NOT NULL,
  CHANGE COLUMN refid ref_id BIGINT NOT NULL,
  CHANGE COLUMN tablename table_name VARCHAR(64) NOT NULL,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE user_addresses
  CHANGE COLUMN userid user_id BIGINT NOT NULL,
  CHANGE COLUMN isdefault is_default VARCHAR(8) NOT NULL DEFAULT '否',
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE support_chats
  CHANGE COLUMN userid user_id BIGINT NOT NULL,
  CHANGE COLUMN adminid admin_id BIGINT NULL,
  CHANGE COLUMN isreply is_reply INT NOT NULL DEFAULT 0,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE user_gallery
  CHANGE COLUMN userid user_id BIGINT NOT NULL,
  CHANGE COLUMN photoUrl photo_url VARCHAR(255) NULL,
  CHANGE COLUMN takenAt taken_at VARCHAR(64) NULL,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE trip_plans
  CHANGE COLUMN userid user_id BIGINT NOT NULL,
  CHANGE COLUMN addtime created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
