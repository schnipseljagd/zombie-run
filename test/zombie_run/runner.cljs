(ns zombie-run.runner
  (:require [doo.runner :refer-macros [doo-all-tests]]
            [zombie-run.world-test]
            [zombie-run.game-test]
            [zombie-run.spec-check-test]))

(doo-all-tests #"zombie-run\..*-(test|check)")
