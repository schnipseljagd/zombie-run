(ns zombie-run.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [zombie-run.world]))

(doo-tests 'zombie-run.world)
