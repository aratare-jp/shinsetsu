CREATE TABLE IF NOT EXISTS "tab_tag"
(
    tab_id  uuid NOT NULL,
    tag_id  uuid NOT NULL,
    created timestamptz DEFAULT now(),
    updated timestamptz DEFAULT now(),
    PRIMARY KEY (tab_id, tag_id),
    CONSTRAINT tab_id FOREIGN KEY (tab_id) REFERENCES "tab" (id),
    CONSTRAINT tag_id FOREIGN KEY (tag_id) REFERENCES "tag" (id)
)