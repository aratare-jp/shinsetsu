(ns harpocrates.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [harpocrates.core-test]))

(doo-tests 'harpocrates.core-test)

