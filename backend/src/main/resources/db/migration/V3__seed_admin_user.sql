INSERT INTO yonghu(yonghuming, mima, xingming, touxiang, xingbie, lianxidianhua, money, shimingrenzheng)
SELECT 'admin',
       '$2a$10$GQ4O/OwNBZiVyk5fX0xRO.9k49I7QeVTRxl6xQj6/TI2vgdT4zqCW',
       '管理员',
       '',
       '男',
       NULL,
       0,
       '已认证'
WHERE NOT EXISTS (SELECT 1 FROM yonghu WHERE yonghuming = 'admin');
