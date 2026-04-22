-- 修复历史数据：姓名为“苏林”但用户名误存为手机号。
-- 先确保手机号落在联系电话字段，再将用户名修正为苏林。
UPDATE users
SET lianxidianhua = yonghuming
WHERE xingming = '苏林'
  AND yonghuming REGEXP '^1[0-9]{10}$'
  AND (lianxidianhua IS NULL OR TRIM(lianxidianhua) = '');

UPDATE users
SET yonghuming = '苏林'
WHERE xingming = '苏林'
  AND yonghuming REGEXP '^1[0-9]{10}$';
