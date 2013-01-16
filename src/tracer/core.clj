(ns tracer.core
  (require [clojure.string :refer [split]]))

(def level (atom 0))

(defn print-level []
  (doseq [i (range @level)]
    (print "  "))
  (print "|--"))

(defn parse-ns-name [f]
  (let [full-class-name (-> f type .getName)
        [ns-name fn-name] (vec (.split full-class-name "\\$"))
        fn-name (.replaceAll fn-name "_QMARK_" "?")
        fn-name (.replaceAll fn-name "_BANG_" "!")
        fn-name (.replaceAll fn-name "_STAR_" "*")
        fn-name (.replaceAll fn-name "_" "-")]
    [ns-name fn-name]))

(defmacro wrap-fn [f]
  `(alter-var-root ~f (fn [original#]
                        (fn [& args#]
                          (let [[ns-name# fn-name#] (parse-ns-name  original#)
                                display-fn-name# (str ns-name# "$" fn-name#)]
                            (print-level)
                            ;; incr the level
                            (swap! level inc)
                            (println display-fn-name#  args#)
                            (let [ret# (apply original# args#)]
                              ;; decr the level
                              (swap! level dec)
                              ret#))))))

(defn trace [ns-name-sym]
  (let [vars (ns-interns ns-name-sym)]
     (doseq [[var-name var-obj] vars
             :let [var-name (name var-name)
                   var-ns (-> var-obj meta :ns .getName name)]]
      (if-not (= "log" var-name)
        (wrap-fn var-obj)))))

