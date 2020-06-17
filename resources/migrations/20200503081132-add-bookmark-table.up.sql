CREATE TABLE bookmarks
(
    id        uuid        DEFAULT uuid_generate_v4(),
    url       text NOT NULL,
    timestamp timestamptz DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
