CREATE
EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    username TEXT NOT NULL UNIQUE
    CHECK
(
    length
(
    username
) BETWEEN 3 AND 128),
    password_hash TEXT NOT NULL
    CHECK
(
    length
(
    password_hash
) BETWEEN 20 AND 255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
(
)
    );

COMMENT
ON TABLE users IS 'Пользователи сервиса (логин/пароль).';
COMMENT
ON COLUMN users.username IS 'Уникальный логин пользователя.';
COMMENT
ON COLUMN users.password_hash IS 'Хэш пароля (bcrypt/argon2).';

CREATE TABLE IF NOT EXISTS auth_tokens
(
    token_hash
    BYTEA
    PRIMARY
    KEY
    CHECK (
    octet_length
(
    token_hash
) = 32),
    user_id UUID NOT NULL REFERENCES users
(
    id
) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
(
),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    ip_hash BYTEA CHECK
(
    ip_hash
    IS
    NULL
    OR
    octet_length
(
    ip_hash
) = 32),
    user_agent TEXT
    );

CREATE INDEX IF NOT EXISTS idx_auth_tokens_user_id ON auth_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_expires_at ON auth_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_revoked_at ON auth_tokens (revoked_at);

COMMENT
ON TABLE auth_tokens IS 'Активные сессии: хэш токена, TTL и статус.';

CREATE TABLE IF NOT EXISTS files
(
    id
    UUID
    PRIMARY
    KEY
    DEFAULT
    gen_random_uuid
(
),
    user_id UUID NOT NULL REFERENCES users
(
    id
) ON DELETE CASCADE,
    filename TEXT NOT NULL CHECK
(
    length
(
    filename
) BETWEEN 1 AND 255),
    size_bytes BIGINT NOT NULL CHECK
(
    size_bytes
    >=
    0
),
    content_type TEXT NOT NULL,
    checksum_sha256 BYTEA CHECK
(
    checksum_sha256
    IS
    NULL
    OR
    octet_length
(
    checksum_sha256
) = 32),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now
(
),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now
(
)
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_files_user_filename
    ON files (user_id, filename);

CREATE INDEX IF NOT EXISTS idx_files_user_uploaded_at
    ON files (user_id, uploaded_at DESC);

CREATE INDEX IF NOT EXISTS idx_files_checksum
    ON files (checksum_sha256);

CREATE
OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at
:= now();
RETURN NEW;
END$$;

DROP TRIGGER IF EXISTS set_updated_at ON files;
CREATE TRIGGER set_updated_at
    BEFORE UPDATE
    ON files
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TABLE IF NOT EXISTS file_blobs
(
    file_id
    UUID
    PRIMARY
    KEY
    REFERENCES
    files
(
    id
) ON DELETE CASCADE,
    data BYTEA NOT NULL
    );

COMMENT
ON TABLE file_blobs IS 'Содержимое файла (BYTEA).';
