CREATE TABLE IF NOT EXISTS "users"
(
    id       uuid           NOT NULL DEFAULT uuid_generate_v4(),
    username varchar UNIQUE NOT NULL,
    password varchar        NOT NULL,
    created  timestamptz             DEFAULT now(),
    updated  timestamptz             DEFAULT now(),
    PRIMARY KEY (id)
)
