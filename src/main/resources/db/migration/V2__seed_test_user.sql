
INSERT INTO users (id, username, password_hash, created_at)
VALUES (
           gen_random_uuid(),
           'user1',
           crypt('pass', gen_salt('bf')),
           now()
       )
    ON CONFLICT (username) DO NOTHING;
