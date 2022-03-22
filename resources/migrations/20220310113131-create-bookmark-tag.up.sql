CREATE TABLE IF NOT EXISTS "bookmark_tag"
(
    bookmark_id uuid NOT NULL,
    tag_id      uuid NOT NULL,
    user_id     uuid NOT NULL,
    created     timestamptz DEFAULT now(),
    updated     timestamptz DEFAULT now(),
    PRIMARY KEY (bookmark_id, tag_id),
    CONSTRAINT fk_bookmark FOREIGN KEY (bookmark_id) REFERENCES "bookmark" (id) ON DELETE CASCADE,
    CONSTRAINT fk_tag FOREIGN KEY (tag_id) REFERENCES "tag" (id) ON DELETE CASCADE,
    CONSTRAINT fk_owner FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
)
