(ns harpocrates.db.protocols.user-db)

(defrecord User [id email password tabs])

(defprotocol UserDAO
  (create [user])
  (read [user])
  (update [user])
  (delete [user]))
