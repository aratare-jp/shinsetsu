CREATE TABLE IF NOT EXISTS "bookmark"
(
    id        uuid    NOT NULL,
    title     varchar NOT NULL,
    url       varchar NOT NULL,
    image     bytea,
    created   timestamptz DEFAULT now(),
    updated   timestamptz DEFAULT now(),
    owner_id  uuid    NOT NULL,
    owner_tab uuid    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT owner_id FOREIGN KEY (owner_id) REFERENCES "user" (id),
    CONSTRAINT owner_tab FOREIGN KEY (owner_id) REFERENCES "tab" (id)
)