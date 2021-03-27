CREATE TABLE IF NOT EXISTS "user"
(
    id       uuid           NOT NULL,
    username varchar UNIQUE NOT NULL,
    password varchar        NOT NULL,
    image    bytea,
    created  timestamptz DEFAULT now(),
    updated  timestamptz DEFAULT now(),
    PRIMARY KEY (id)
)