CREATE TABLE IF NOT EXISTS "tag"
(
    id       uuid    NOT NULL,
    name     varchar NOT NULL,
    color    varchar(10) DEFAULT NULL,
    created  timestamptz DEFAULT now(),
    updated  timestamptz DEFAULT now(),
    owner_id uuid    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT owner_id FOREIGN KEY (owner_id) REFERENCES "user" (id)
)