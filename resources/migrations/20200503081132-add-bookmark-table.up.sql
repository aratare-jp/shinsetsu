CREATE TABLE bookmarks
(
    id        uuid        DEFAULT uuid_generate_v4(),
    url       text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id uuid NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bookmark
    FOREIGN KEY (user_id) REFERENCES user(id)
);
