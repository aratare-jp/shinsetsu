CREATE TABLE IF NOT EXISTS "tag"
(
    id      uuid    NOT NULL,
    name    varchar NOT NULL,
    colour  varchar     DEFAULT NULL,
    created timestamptz DEFAULT now(),
    updated timestamptz DEFAULT now(),
    user_id uuid    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT owner_id FOREIGN KEY (user_id) REFERENCES "user" (id)
)