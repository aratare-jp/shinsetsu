CREATE TABLE users
(
    id         uuid DEFAULT uuid_generate_v4(),
    first_name text,
    last_name  text,
    email      text,
    last_login timestamp,
    is_active  boolean,
    password   text,
    PRIMARY KEY (id)
);
