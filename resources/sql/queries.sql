-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(first_name, last_name, email, password)
VALUES (:user/first-name, :user/last-name, :user/email, :user/password)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :user/first-name, last_name = :user/last-name, email = :user/email
WHERE id = :user/id

-- :name get-user! :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :user/id

-- :name get-user-by-email! :? :1
-- :doc retrieves a user record given the email
SELECT * FROM users
WHERE email = :user/email

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :user/id

-- :name get-bookmark! :? :1
-- :doc retrieves a bookmark record given the id
SELECT * FROM bookmarks
WHERE id = :bookmark/id

-- :name create-bookmark! :! :n
-- :doc creates a new bookmark record
INSERT INTO bookmarks
(url)
VALUES (:bookmark/url)

-- :name create-user-bookmark! :! :n
-- :doc creates a new bookmark record
INSERT INTO users_bookmarks
(user_id, bookmark_id)
VALUES (:user/user-id, :bookmark/bookmark-id)

-- :name get-bookmark-from-user! :? :1
-- :doc retrieves a bookmark record given the user id
SELECT * FROM bookmarks
WHERE id IN (SELECT bookmark_id FROM users_bookmarks WHERE user_id = :user/id)
