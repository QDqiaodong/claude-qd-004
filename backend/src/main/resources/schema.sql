-- 奶牛场：建表 + 种子数据
-- 表结构由 Hibernate 兜底（ddl-auto=update），这里只保证首次启动就有数据

CREATE TABLE IF NOT EXISTS barn (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  kind VARCHAR(16) NOT NULL DEFAULT '产奶舍',
  capacity INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT '在用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_barn_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS milking_stall (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  barn_id BIGINT NULL,
  stall_type VARCHAR(16) NOT NULL DEFAULT '并列式',
  status VARCHAR(16) NOT NULL DEFAULT '可用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stall_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cow (
  id BIGINT NOT NULL AUTO_INCREMENT,
  ear_tag VARCHAR(32) NOT NULL,
  nickname VARCHAR(32) NULL,
  breed VARCHAR(32) NULL,
  lactation VARCHAR(16) NOT NULL DEFAULT '泌乳中',
  barn_id BIGINT NULL,
  status VARCHAR(16) NOT NULL DEFAULT '在栏',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cow_ear_tag (ear_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS milking_shift (
  id BIGINT NOT NULL AUTO_INCREMENT,
  shift_no VARCHAR(32) NOT NULL,
  milking_date DATE NOT NULL,
  period VARCHAR(16) NOT NULL,
  stall_id BIGINT NOT NULL,
  barn_id BIGINT NOT NULL,
  milker VARCHAR(32) NOT NULL,
  start_min INT NOT NULL,
  end_min INT NOT NULL,
  milk_kg DOUBLE NULL,
  status VARCHAR(16) NOT NULL DEFAULT '待开挤',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shift_no (shift_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS feed (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  unit VARCHAR(16) NOT NULL DEFAULT '公斤',
  stock INT NOT NULL DEFAULT 0,
  warn_stock INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT '在用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_feed_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS feed_issue (
  id BIGINT NOT NULL AUTO_INCREMENT,
  barn_id BIGINT NOT NULL,
  feed_id BIGINT NOT NULL,
  qty INT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  operator VARCHAR(32) NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 原奶抽检台账：一条流水要么是抽检（体细胞数 + 结论），要么是处置。
-- 只能挂在已收班（已完成）的班次上，收班公斤数不动。
CREATE TABLE IF NOT EXISTS milk_test (
  id BIGINT NOT NULL AUTO_INCREMENT,
  shift_id BIGINT NOT NULL,
  record_type VARCHAR(16) NOT NULL,
  somatic_cells BIGINT NULL,
  result VARCHAR(8) NULL,
  disposition VARCHAR(8) NULL,
  operator VARCHAR(32) NULL,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_milk_test_shift (shift_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO barn (id, code, name, kind, capacity, status) VALUES
  (1, 'BN-01', '产奶一舍', '产奶舍', 60, '在用'),
  (2, 'BN-02', '产奶二舍', '产奶舍', 60, '在用'),
  (3, 'BN-03', '犊牛舍',   '犊牛舍', 30, '在用'),
  (4, 'BN-04', '干奶舍',   '干奶舍', 40, '在用'),
  (5, 'BN-05', '隔离舍',   '隔离舍', 10, '停用');

INSERT IGNORE INTO milking_stall (id, code, name, barn_id, stall_type, status) VALUES
  (1, 'MS-01', '一号挤奶位', 1, '并列式', '可用'),
  (2, 'MS-02', '二号挤奶位', 1, '并列式', '可用'),
  (3, 'MS-03', '三号挤奶位', 2, '并列式', '可用'),
  (4, 'MS-04', '四号挤奶位', 2, '转盘式', '可用'),
  (5, 'MS-05', '五号挤奶位', 2, '并列式', '维修');

INSERT IGNORE INTO cow (id, ear_tag, nickname, breed, lactation, barn_id, status) VALUES
  (1, 'DH-1001', '花妞', '荷斯坦', '泌乳中', 1,    '在栏'),
  (2, 'DH-1002', '大黑', '荷斯坦', '泌乳中', 1,    '在栏'),
  (3, 'DH-1003', '小黄', '荷斯坦', '泌乳中', 2,    '在栏'),
  (4, 'DH-1004', '雪球', '娟姗',   '泌乳中', 2,    '在栏'),
  (5, 'DH-1005', '老白', '荷斯坦', '干奶期', 4,    '在栏'),
  (6, 'DH-1006', '点点', '荷斯坦', '待产',   4,    '在栏'),
  (7, 'DH-1007', '铁蛋', '荷斯坦', '已淘汰', NULL, '离栏'),
  (8, 'DH-1008', '二妞', '荷斯坦', '泌乳中', 2,    '在栏');

INSERT IGNORE INTO milking_shift (id, shift_no, milking_date, period, stall_id, barn_id, milker, start_min, end_min, milk_kg, status, created_at, updated_at) VALUES
  (1, 'MS-0001', CURDATE(), '早班', 1, 1, '王师傅', 300,  420,  1520.5, '已完成', NOW(), NOW()),
  (2, 'MS-0002', CURDATE(), '早班', 3, 2, '李师傅', 300,  420,  1480.0, '已完成', NOW(), NOW()),
  (3, 'MS-0003', CURDATE(), '中班', 2, 1, '王师傅', 780,  900,  NULL,   '待开挤', NOW(), NOW()),
  (4, 'MS-0004', CURDATE(), '中班', 5, 2, '赵师傅', 780,  900,  NULL,   '已取消', NOW(), NOW()),
  (5, 'MS-0005', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '晚班', 4, 2, '赵师傅', 1200, 1320, 960.0, '已完成', NOW(), NOW()),
  (6, 'MS-0006', CURDATE(), '早班', 4, 2, '孙师傅', 300,  420,  1005.0, '已完成', NOW(), NOW()),
  (7, 'MS-0007', CURDATE(), '中班', 3, 2, '李师傅', 780,  900,  NULL,   '挤奶中', NOW(), NOW());

INSERT IGNORE INTO feed (id, code, name, unit, stock, warn_stock, status) VALUES
  (1, 'FD-1001', '苜蓿干草',   '捆',   120, 30,  '在用'),
  (2, 'FD-1002', '玉米青贮',   '公斤', 800, 200, '在用'),
  (3, 'FD-1003', '豆粕',       '公斤', 300, 80,  '在用'),
  (4, 'FD-1004', '预混料',     '袋',   12,  20,  '在用'),
  (5, 'FD-1005', '旧配方精料', '袋',   5,   0,   '停用');

INSERT IGNORE INTO feed_issue (id, barn_id, feed_id, qty, kind, operator, created_at) VALUES
  (1, 1, 1, 4,   '领用', '王师傅', NOW()),
  (2, 1, 1, 1,   '退料', '王师傅', NOW()),
  (3, 2, 2, 500, '领用', '李师傅', NOW()),
  (4, 1, 2, 200, '领用', '王师傅', NOW()),
  (5, 4, 5, 2,   '领用', '赵师傅', NOW());

INSERT IGNORE INTO milk_test (id, shift_id, record_type, somatic_cells, result, disposition, operator, remark, created_at) VALUES
  (1, 1, '抽检', 180000,  '合格',   NULL, '化验室', '体细胞正常，结案',      NOW()),
  (2, 2, '抽检', 620000,  '不合格', NULL, '化验室', '体细胞超标',            NOW()),
  (3, 2, '处置', NULL,    NULL,     '扣留', '化验室', '先扣留，等复检',       NOW());
