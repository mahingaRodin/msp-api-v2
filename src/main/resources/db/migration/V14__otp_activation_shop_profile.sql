-- Customer OTP, activation invites, shop cart/favorites, profile, admin inbox

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS otp_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS otp_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS profile_picture TEXT;

UPDATE users SET email_verified = TRUE WHERE user_status = 'ACTIVE';

ALTER TABLE tenant_registrations
    ADD COLUMN IF NOT EXISTS more_info_message TEXT;

CREATE TABLE IF NOT EXISTS activation_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose VARCHAR(40) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_activation_token ON activation_tokens (token);

CREATE TABLE IF NOT EXISTS product_favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_favorite_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches (id) ON DELETE SET NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE IF NOT EXISTS admin_notifications (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    registration_id UUID,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_admin_notifications_created ON admin_notifications (created_at DESC);
