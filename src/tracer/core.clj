(ns tracer.core
  (require [clojure.string :refer [split]]))

(def ^:private level-in-threads (atom {}))

(defn- print-level [level]
  (doseq [i (range level)]
    (print "  "))
  (print "|-+ "))

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

(defmacro wrap-fn [f show-tid?]
  `(do
     (alter-meta! ~f assoc ::orig (deref ~f))
     (alter-var-root ~f (fn [original#]
                        (fn [& args#]
                          (let [[ns-name# fn-name#] (parse-ns-name  original#)
                                display-fn-name# (str ns-name# "/" fn-name#)
                                display-msg# (pr-str (cons (symbol display-fn-name#) args#))
                                tid# (.getId (Thread/currentThread))
                                level# (or (@level-in-threads tid#)
                                           ((swap! level-in-threads assoc tid# 0) tid#))]
                            (when ~show-tid? (print (format "%d:  " tid#)))
                            (print-level level#)
                            ;; incr the level
                            (swap! level-in-threads update-in [tid#] inc)
                            (println display-msg#)
                            (let [ret# (apply original# args#)]
                              ;; decr the level
                              (swap! level-in-threads update-in [tid#] dec)
                              ret#)))))))

(defn unwrap-fn [v]
  (when (::orig (meta v))
    (doto v
      (alter-var-root (constantly ((meta v) ::orig)))
      (alter-meta! dissoc ::orig))))

(defn trace
  "Tell tracer which namespace you want to trace.
  supported flags:
    :show-tid - print the thread id when functions has been called."
  [ns-name-sym & flags]
  (if ('#{tracer.core} ns-name-sym)
    (println "ns:" ns-name-sym "is forbidden to be traced!")
    (let [vars (ns-interns ns-name-sym)]
      (doseq [[var-name  var-obj] vars]
        (when (callable? var-obj)
          (println (format "Add %s/%s to trace list." (name ns-name-sym)  var-name))
          (wrap-fn var-obj ((set flags) :show-tid)))))))

(defn untrace [ns-name-sym]
  (doseq [[_ var-obj] (ns-interns ns-name-sym)]
    (when (callable? var-obj)
      (unwrap-fn var-obj))))
