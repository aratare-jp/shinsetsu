CREATE TABLE IF NOT EXISTS "current_user"
(
    id      uuid    NOT NULL,
    token   varchar NOT NULL,
    created timestamptz DEFAULT now(),
    updated timestamptz DEFAULT now(),
    PRIMARY KEY (id, token),
    CONSTRAINT fk_user FOREIGN KEY (id) REFERENCES "user" (id)
)