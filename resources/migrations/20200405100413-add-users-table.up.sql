CREATE TABLE users
(
    id         uuid        DEFAULT uuid_generate_v4(),
    first_name text        NOT NULL,
    last_name  text        NOT NULL,
    email      text UNIQUE NOT NULL,
    password   text        NOT NULL,
    timestamp  timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active  boolean     DEFAULT TRUE,
    PRIMARY KEY (id)
);
