(ns app.mutations
  (:require
    [app.resolvers :refer [list-table]]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(pc/defmutation delete-person [env {list-id :list/id person-id :person/id}]
                ;; Pathom registers these mutations in an index. The key that the mutation is
                ;; indexed by can be overridden with the `::pc/sym`
                ;; configuration option below. Note, however, that mutation we are sending
                ;; in the `comp/transact!` from the PersonList component above is
                ;; `[(api/delete-person ,,,)]` which will expand to the fully qualified
                ;; mutation of `[(app.mutations/delete-person ,,,)]`. If you
                ;; encounter unexpected error messages about mutations not being found,
                ;; ensure any overridden syms match the expanded namespaces of your mutations.
                {::pc/sym `delete-person}
                (log/info "Deleting person" person-id "from list" list-id)
                (swap! list-table update list-id update :list/people (fn [old-list] (filterv #(not= person-id %) old-list))))

(def mutations [delete-person])
