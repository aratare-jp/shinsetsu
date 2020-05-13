CREATE TABLE users
(
    id         uuid DEFAULT uuid_generate_v4(),
    first_name text        NOT NULL,
    last_name  text        NOT NULL,
    email      text UNIQUE NOT NULL,
    last_login timestamp,
    is_active  boolean,
    password   text,
    PRIMARY KEY (id)
);
