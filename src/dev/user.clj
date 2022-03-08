(ns user
  (:require
    [shinsetsu.server :as server]
    [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs refresh]]))

;; Ensure we only refresh the source we care about. This is important
;; because `resources` is on our classpath and we don't want to
;; accidentally pull source from there when cljs builds cache files there.
(set-refresh-dirs "src/dev" "src/main")

(defn start []
  (server/start))

(defn restart
  "Stop the server, reload all source code, then restart the server.

  See documentation of tools.namespace.repl for more information."
  []
  (server/stop)
  (refresh :after 'user/start))

;; These are here so we can run them from the editor with kb shortcuts.  See IntelliJ's "Send Top Form To REPL" in
;; keymap settings.
(comment
  (start)
  (restart))
