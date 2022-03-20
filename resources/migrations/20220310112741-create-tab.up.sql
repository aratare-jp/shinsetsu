CREATE TABLE IF NOT EXISTS "tab"
(
    id              uuid    NOT NULl DEFAULT uuid_generate_v4(),
    name            varchar NOT NULL,
    password        varchar          DEFAULT NULL,
    "is-protected?" bool             DEFAULT FALSE,
    created         timestamptz      DEFAULT now(),
    updated         timestamptz      DEFAULT now(),
    "user-id"       uuid    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT owner_id FOREIGN KEY ("user-id") REFERENCES "user" (id)
)
