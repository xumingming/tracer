(ns tracer.core
  (:require [colorize.ansi :refer [colorize ansi-colors]]))

(defn color-println
  "print with color"
  [level with-color? str]
  (if with-color?
    (let [colors (keys (dissoc ansi-colors :white :black :default))
          color  (nth colors (mod level (count colors)))]
      (println (colorize str {:fg color})))
    (println str)))

(defonce ^:private level-in-threads (atom {}))

(defonce wrapped->orig (atom {}))

(defn traced?
  "Returns whether the specified object is traced."
  [obj]
  (boolean (@wrapped->orig obj)))

(defn get-orig
  "Returns the original object if the specified obj is traced, otherwise returns itself."
  [obj]
  (if (traced? obj)
    (@wrapped->orig obj)
    obj))

(defn- print-trace [call-msg level tid with-color?]
  (locking ::lock
    (let [prefix (str (and tid (format "%d: " tid))
                      (apply str (repeat level "| "))
                      "|- ")]
      (color-println level with-color? (str prefix call-msg)))))

(defn- print-trace-end [ret level tid with-color?]
  (locking ::lock
    (let [prefix (str (and tid (format "%d: " tid))
                      (apply str (repeat level "| "))
                      " \\=> ")]
      (color-println level with-color? (str prefix ret)))))

(defn parse-ns-name [f]
  (let [full-class-name   (-> f type .getName)
        [ns-name fn-name] (vec (.split full-class-name "\\$"))
        fn-name           (-> fn-name
                              (.replaceAll "_QMARK_" "?")
                              (.replaceAll "_BANG_" "!")
                              (.replaceAll "_STAR_" "*")
                              (.replaceAll "_"      "-"))]
    [ns-name fn-name]))

(defn- callable? [var-obj]
  (boolean (:arglists (meta var-obj))))

(defn- remove-private-meta [var-obj]
  )

(defn build-wrapped-fn [f show-tid? with-color?]
  (fn [& args]
    (let [[ns-name fn-name] (parse-ns-name f)
          display-fn-name   (str ns-name "/" fn-name)
          args              (map get-orig args)
          display-msg       (pr-str (cons (symbol display-fn-name) args))
          tid               (.getId (Thread/currentThread))
          level             (or (@level-in-threads tid)
                                ((swap! level-in-threads assoc tid 0) tid))]
      (print-trace display-msg level (and show-tid? tid) with-color?)
      ;; incr the level
      (swap! level-in-threads update-in [tid] inc)
      (try
        (let [ret (apply f args)
              display-ret (get-orig ret)]
          (print-trace-end (pr-str display-ret) level (and show-tid? tid) with-color?)
          ;; decr the level
          (swap! level-in-threads update-in [tid] dec)
          ret)
        (catch Exception e
          ;; reset level to 0 if there is exception
          (swap! level-in-threads assoc-in [tid] 0)
          ;; rethrow the exception
          (throw e))))))

(defmacro wrap-fn [f show-tid? with-color?]
  `(let [wrapped-fn# (build-wrapped-fn (deref ~f) ~show-tid? ~with-color?)]
     ;; save the wrapped to orig mapping
     (swap! wrapped->orig assoc wrapped-fn# (deref ~f))
     ;; alter var's root to wrapped function
     (alter-var-root ~f (constantly wrapped-fn#))
     ;; remove the :private meta if it exists
     (alter-meta! ~f (fn [curr-meta#]
                       (-> curr-meta#
                           (assoc :tracer-private (boolean (:private curr-meta#)))
                           (dissoc :private))))
     ))

(defn unwrap-fn
  [v]
  (when (traced? @v)
    ;; alter the var back
    (alter-var-root v (constantly (get-orig @v)))
    ;; remove from the cache
    (swap! wrapped->orig dissoc @v)
    ;; setback the :private meta
    (alter-meta! v (fn [curr-meta]
                      (-> curr-meta
                          (assoc :private (boolean (:tracer-private curr-meta)))
                          (dissoc :tracer-private))))))

(defn trace
  "Tell tracer which namespace you want to trace.
  supported flags:
    :show-tid - print the thread id when functions has been called."
  [ns-name-sym & flags]
  (if ('#{tracer.core colorize.ansi} ns-name-sym)
    (println "ns:" ns-name-sym "is forbidden to be traced!")
    (let [vars (ns-interns ns-name-sym)]
      (doseq [[var-name  var-obj] vars]
        (when (and (callable? var-obj) (not (traced? (deref var-obj))))
          (println (format "Add %s/%s to trace list." (name ns-name-sym) var-name))
          (let [flags       (set flags)
                show-tid?   (flags :show-tid)
                with-color? (flags :with-color)]
            (wrap-fn var-obj show-tid? with-color?))))
      ;; re-use the traced namespace -- so that some private function
      ;; will be accessible in the REPL
      (use ns-name-sym))))

(defn untrace
  "Tell tracer to un-trace the objects(functions, macros) in the specified namespace."
  [ns-name-sym]
  (doseq [[var-name var-obj] (ns-interns ns-name-sym)]
    (when (traced? (deref var-obj))
      (println (format "Remove %s/%s from trace list." (name ns-name-sym) var-name))
      (unwrap-fn var-obj)))
  ;; re-use the traced namespace -- so that some private function
  ;; will be accessible in the REPL
  (use ns-name-sym))
