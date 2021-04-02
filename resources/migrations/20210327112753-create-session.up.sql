CREATE TABLE IF NOT EXISTS "session"
(
    user_id uuid    NOT NULL,
    token   varchar NOT NULL,
    expired timestamptz DEFAULT NULL,
    created timestamptz DEFAULT now(),
    updated timestamptz DEFAULT now(),
    PRIMARY KEY (user_id, token),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES "user" (id)
)