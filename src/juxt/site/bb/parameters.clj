;; Copyright © 2023, JUXT LTD.

(ns juxt.site.bb.parameters
  (:require [juxt.site.bb.user-input :as input]))

(defn resolve-parameters [parameters args]
  (reduce
   (fn [acc [parameter {:keys [label default choices password]}]]
     (assoc acc parameter
            (or (get args parameter)
                default
                (cond
                  choices (let [choice-v (map (juxt #(format "%s (%s)" (:label %) (:value %)) identity) choices)
                                choice (input/choose (map first choice-v) {:header (format "Choose %s" parameter)})

                                value (get-in (into {} choice-v) [choice :value])]
                            value)

                  password (input/input {:header (or label parameter)
                                         :password true})

                  :else (input/input {:header (or label parameter)})))))
   {}
   parameters))
