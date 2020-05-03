CREATE TABLE bookmarks
(
    id uuid DEFAULT uuid_generate_v4 (),
    url TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id)
);
