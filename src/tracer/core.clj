(ns tracer.core
  (require [clojure.string :refer [split]]))

(def ^:private level-in-threads (atom {}))

(defn- print-level [level]
  (doseq [i (range level)]
    (print "  "))
  (print "|--"))

(defn- parse-ns-name [f]
  (let [full-class-name (-> f type .getName)
        [ns-name fn-name] (vec (.split full-class-name "\\$"))
        fn-name (.replaceAll fn-name "_QMARK_" "?")
        fn-name (.replaceAll fn-name "_BANG_" "!")
        fn-name (.replaceAll fn-name "_STAR_" "*")
        fn-name (.replaceAll fn-name "_" "-")]
    [ns-name fn-name]))

(defn- callable? [var-obj]
  (not (nil? (:arglists (meta var-obj)))))

(defmacro wrap-fn [f]
  `(alter-var-root ~f (fn [original#]
                        (fn [& args#]
                          (let [[ns-name# fn-name#] (parse-ns-name  original#)
                                display-fn-name# (str ns-name# "$" fn-name#)
                                tid# (.getId (Thread/currentThread))
                                level# (or (@level-in-threads tid#)
                                           ((swap! level-in-threads assoc tid# 0) tid#))]
                            (print-level level#)
                            ;; incr the level
                            (swap! level-in-threads update-in [tid#] inc)
                            (println display-fn-name#  args#)
                            (let [ret# (apply original# args#)]
                              ;; decr the level
                              (swap! level-in-threads update-in [tid#] dec)
                              ret#))))))

(defn trace [ns-name-sym]
  (let [vars (ns-interns ns-name-sym)]
    (doseq [[var-name  var-obj] vars]
      (when (callable? var-obj)
        (println "Add" var-name "to trace list.")
        (wrap-fn var-obj)))))
