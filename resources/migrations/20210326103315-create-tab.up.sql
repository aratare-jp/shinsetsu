CREATE TABLE IF NOT EXISTS "tab"
(
    id        uuid    NOT NULL,
    name      varchar NOT NULL,
    password  varchar     DEFAULT NULL,
    created   timestamptz DEFAULT now(),
    updated   timestamptz DEFAULT now(),
    owner_id  uuid    NOT NULL,
    owner_tab uuid    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT owner_id FOREIGN KEY (owner_id) REFERENCES "user" (id)
)