CREATE DATABASE IF NOT EXISTS user_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_service.users (
  id CHAR(36) NOT NULL,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  role INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_service.orders (
  id CHAR(36) NOT NULL,
  user_id CHAR(36) NOT NULL,
  ticket_item_id CHAR(36) NOT NULL,
  quantity INT NOT NULL,
  unit_price BIGINT NOT NULL,
  total_price BIGINT NOT NULL,
  status INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  KEY idx_orders_user_id (user_id),
  KEY idx_orders_ticket_item_id (ticket_item_id),
  KEY idx_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_service.payments (
  id CHAR(36) NOT NULL,
  order_id CHAR(36) NOT NULL,
  user_id CHAR(36) NULL,
  amount BIGINT NOT NULL,
  payment_method VARCHAR(36) NOT NULL,
  status INT NOT NULL DEFAULT 0,
  transaction_id CHAR(36) NOT NULL,
  paid_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payments_transaction_id (transaction_id),
  KEY idx_payments_order_id (order_id),
  KEY idx_payments_user_id (user_id),
  KEY idx_payments_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO user_service.users
  (id, username, email, password, role, created_at, updated_at, deleted_at)
VALUES
  (
    '10000000-0000-4000-8000-000000000001',
    'rail.admin',
    'admin@vetau.local',
    '$2b$12$s86Eq86xvHB0rF0DfwxlR.kTurABdVoewEjJcJIXPFEkecSv3Rr3S',
    1,
    '2026-05-14 08:00:00.000000',
    '2026-05-14 08:00:00.000000',
    NULL
  ),
  (
    '10000000-0000-4000-8000-000000000002',
    'rail.customer',
    'customer@vetau.local',
    '$2b$12$xXhcTxXw7qKM0iPWZlkpaOel3hmw2uSooNSoma8k0GcrOXZcZGSJO',
    0,
    '2026-05-14 08:05:00.000000',
    '2026-05-14 08:05:00.000000',
    NULL
  ),
  (
    '10000000-0000-4000-8000-000000000003',
    'rail.member',
    'member@vetau.local',
    '$2b$12$xXhcTxXw7qKM0iPWZlkpaOel3hmw2uSooNSoma8k0GcrOXZcZGSJO',
    0,
    '2026-05-14 08:10:00.000000',
    '2026-05-14 08:10:00.000000',
    NULL
  )
ON DUPLICATE KEY UPDATE
  id = VALUES(id),
  username = VALUES(username),
  email = VALUES(email),
  password = VALUES(password),
  role = VALUES(role),
  updated_at = VALUES(updated_at),
  deleted_at = VALUES(deleted_at);

INSERT INTO order_service.orders
  (id, user_id, ticket_item_id, quantity, unit_price, total_price, status, created_at, updated_at, deleted_at)
VALUES
  (
    '40000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000101',
    2,
    790000,
    1580000,
    1,
    '2026-05-14 09:30:00.000000',
    '2026-05-14 09:35:00.000000',
    NULL
  ),
  (
    '40000000-0000-4000-8000-000000000002',
    '10000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000202',
    1,
    760000,
    760000,
    0,
    '2026-05-14 10:15:00.000000',
    '2026-05-14 10:15:00.000000',
    NULL
  ),
  (
    '40000000-0000-4000-8000-000000000003',
    '10000000-0000-4000-8000-000000000003',
    '30000000-0000-4000-8000-000000000301',
    3,
    480000,
    1440000,
    1,
    '2026-05-14 11:00:00.000000',
    '2026-05-14 11:08:00.000000',
    NULL
  ),
  (
    '40000000-0000-4000-8000-000000000004',
    '10000000-0000-4000-8000-000000000002',
    '30000000-0000-4000-8000-000000000401',
    1,
    720000,
    720000,
    3,
    '2026-05-14 12:20:00.000000',
    '2026-05-14 12:55:00.000000',
    NULL
  )
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  ticket_item_id = VALUES(ticket_item_id),
  quantity = VALUES(quantity),
  unit_price = VALUES(unit_price),
  total_price = VALUES(total_price),
  status = VALUES(status),
  updated_at = VALUES(updated_at),
  deleted_at = VALUES(deleted_at);

INSERT INTO payment_service.payments
  (id, order_id, user_id, amount, payment_method, status, transaction_id, paid_at, created_at, updated_at, deleted_at)
VALUES
  (
    '50000000-0000-4000-8000-000000000001',
    '40000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000002',
    1580000,
    'VNPAY',
    1,
    '60000000-0000-4000-8000-000000000001',
    '2026-05-14 09:34:00.000000',
    '2026-05-14 09:31:00.000000',
    '2026-05-14 09:34:00.000000',
    NULL
  ),
  (
    '50000000-0000-4000-8000-000000000002',
    '40000000-0000-4000-8000-000000000002',
    '10000000-0000-4000-8000-000000000002',
    760000,
    'VNPAY',
    0,
    '60000000-0000-4000-8000-000000000002',
    NULL,
    '2026-05-14 10:16:00.000000',
    '2026-05-14 10:16:00.000000',
    NULL
  ),
  (
    '50000000-0000-4000-8000-000000000003',
    '40000000-0000-4000-8000-000000000003',
    '10000000-0000-4000-8000-000000000003',
    1440000,
    'BANK_TRANSFER',
    1,
    '60000000-0000-4000-8000-000000000003',
    '2026-05-14 11:07:00.000000',
    '2026-05-14 11:01:00.000000',
    '2026-05-14 11:07:00.000000',
    NULL
  ),
  (
    '50000000-0000-4000-8000-000000000004',
    '40000000-0000-4000-8000-000000000004',
    '10000000-0000-4000-8000-000000000002',
    720000,
    'VNPAY',
    3,
    '60000000-0000-4000-8000-000000000004',
    '2026-05-14 12:28:00.000000',
    '2026-05-14 12:21:00.000000',
    '2026-05-14 12:55:00.000000',
    NULL
  )
ON DUPLICATE KEY UPDATE
  id = VALUES(id),
  order_id = VALUES(order_id),
  user_id = VALUES(user_id),
  amount = VALUES(amount),
  payment_method = VALUES(payment_method),
  status = VALUES(status),
  transaction_id = VALUES(transaction_id),
  paid_at = VALUES(paid_at),
  updated_at = VALUES(updated_at),
  deleted_at = VALUES(deleted_at);
