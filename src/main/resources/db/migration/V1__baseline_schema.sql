-- V1__baseline_schema.sql
-- Core POS tables required before incremental migrations V4+.
-- Safe on existing databases: every statement is idempotent.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(255),
    role            VARCHAR(50)  NOT NULL,
    password        VARCHAR(255) NOT NULL,
    user_status     VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    store_id        UUID,
    branch_id       UUID,
    suspended_at    TIMESTAMP,
    discharged_at   TIMESTAMP,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    last_login      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stores (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand           VARCHAR(255) NOT NULL,
    store_admin_id  UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    description     VARCHAR(255),
    store_type      VARCHAR(255),
    status          SMALLINT,
    address         VARCHAR(255),
    phone           VARCHAR(255),
    email           VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS branches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255),
    address     VARCHAR(255),
    phone       VARCHAR(255),
    email       VARCHAR(255),
    open_time   TIME,
    close_time  TIME,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    store_id    UUID REFERENCES stores(id),
    manager_id  UUID REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS branch_working_days (
    branch_id    UUID NOT NULL REFERENCES branches(id),
    working_days VARCHAR(255)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_store'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_store FOREIGN KEY (store_id) REFERENCES stores(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_branch'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES branches(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS categories (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name     VARCHAR(255),
    store_id UUID REFERENCES stores(id)
);

CREATE TABLE IF NOT EXISTS products (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    sku            VARCHAR(255) NOT NULL,
    description    VARCHAR(255),
    mrp            DOUBLE PRECISION,
    selling_price  DOUBLE PRECISION,
    brand          VARCHAR(255),
    image          VARCHAR(255),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    category_id    UUID REFERENCES categories(id),
    store_id       UUID REFERENCES stores(id),
    CONSTRAINT products_sku_key UNIQUE (sku)
);

CREATE TABLE IF NOT EXISTS customers (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL,
    phone      VARCHAR(255),
    password   VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT customers_email_key UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_customer_email ON customers(email);

CREATE TABLE IF NOT EXISTS orders (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    total_amount DOUBLE PRECISION,
    created_at   TIMESTAMP,
    payment_type SMALLINT,
    status       VARCHAR(50),
    branch_id    UUID REFERENCES branches(id),
    cashier_id   UUID REFERENCES users(id),
    customer_id  UUID REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quantity   INTEGER,
    price      DOUBLE PRECISION,
    product_id UUID REFERENCES products(id),
    order_id   UUID REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS inventories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quantity    INTEGER NOT NULL,
    last_update TIME,
    branch_id   UUID REFERENCES branches(id),
    product_id  UUID REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS shift_reports (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_start   TIMESTAMP,
    shift_end     TIMESTAMP,
    total_sales   DOUBLE PRECISION,
    total_refunds DOUBLE PRECISION,
    net_sale      DOUBLE PRECISION,
    total_orders  INTEGER NOT NULL,
    branch_id     UUID REFERENCES branches(id),
    cashier_id    UUID REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS refunds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reason          VARCHAR(255),
    amount          DOUBLE PRECISION,
    payment_type    SMALLINT,
    created_at      TIMESTAMP,
    order_id        UUID REFERENCES orders(id),
    shift_report_id UUID REFERENCES shift_reports(id),
    cashier_id      UUID REFERENCES users(id),
    branch_id       UUID REFERENCES branches(id)
);

CREATE TABLE IF NOT EXISTS shift_reports_top_selling_products (
    shift_report_id           UUID NOT NULL REFERENCES shift_reports(id),
    top_selling_products_id   UUID NOT NULL REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS shift_reports_recent_orders (
    shift_report_id  UUID NOT NULL REFERENCES shift_reports(id),
    recent_orders_id UUID NOT NULL REFERENCES orders(id)
);
