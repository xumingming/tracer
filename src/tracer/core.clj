(ns tracer.core)

(defonce ^:private color-print-on (atom false))

(defn set-color-print-on!
  "Enable color print"
  []
  (reset! color-print-on true))

(defn set-color-print-off!
  "Enable color print"
  []
  (reset! color-print-on false))

(defn color-println
  "print with color"
  [level str]
  (if @color-print-on
    (let [colors [ ;;"\u001B[30m" ;; black
                  "\u001B[31m" ;; red
                  "\u001B[32m" ;; green
                  "\u001B[33m" ;; yellow
                  "\u001B[34m" ;; blue
                  "\u001B[35m" ;; purple
                  "\u001B[36m" ;; cyan
                  ;;"\u001B[37m" ;; white
                  ]
          cmd-close "\u001B[m"
          color-cnt (count colors)
          color-ind (mod level color-cnt)
          color (get colors color-ind)]
      (println color  str cmd-close))
    (println str)))

(defonce ^:private level-in-threads (atom {}))
(defonce ^:private print-lock (Object.))

(defonce wrapped->orig (atom {}))

(defn traced?
  "Returns whether the specified object is traced."
  [obj]
  (if (@wrapped->orig obj) true false))

(defn get-orig
  "Returns the original object if the specified obj is traced, otherwise returns itself."
  [obj]
  (if (traced? obj)
    (@wrapped->orig obj)
    obj))

(defn- print-trace [call-msg level tid]
  (locking print-lock
    (let [prefix (str (and tid (format "%d: " tid))
                      (apply str (take level (repeat "| ")))
                      "|- ")]
      (color-println level (str prefix call-msg))))) ;; here

(defn- print-trace-end [ret level tid]
  (locking print-lock
    (let [prefix (str (and tid (format "%d: " tid))
                      (apply str (take level (repeat "| ")))
                      " \\=> ")]
      (color-println level (str prefix ret)))))

(defn parse-ns-name [f]
  (let [full-class-name (-> f type .getName)
        [ns-name fn-name] (vec (.split full-class-name "\\$"))
        fn-name (.replaceAll fn-name "_QMARK_" "?")
        fn-name (.replaceAll fn-name "_BANG_" "!")
        fn-name (.replaceAll fn-name "_STAR_" "*")
        fn-name (.replaceAll fn-name "_" "-")]
    [ns-name fn-name]))

(defn- callable? [var-obj]
  (not (nil? (:arglists (meta var-obj)))))

(defn build-wrapped-fn [f show-tid?]
  (fn [& args]
    (let [[ns-name fn-name] (parse-ns-name f)
          display-fn-name (str ns-name "/" fn-name)
          args (map get-orig args)
          display-msg (pr-str (cons (symbol display-fn-name) args))
          tid (.getId (Thread/currentThread))
          level (or (@level-in-threads tid)
                     ((swap! level-in-threads assoc tid 0) tid))]
      (print-trace display-msg level (and show-tid? tid))
      ;; incr the level
      (swap! level-in-threads update-in [tid] inc)
      (try
        (let [ret (apply f args)
              ret (get-orig ret)]
          (print-trace-end (pr-str ret) level (and show-tid? tid))
          ;; decr the level
          (swap! level-in-threads update-in [tid] dec)
          ret)
        (catch Exception e
          ;; reset level to 0 if there is exception
          (swap! level-in-threads update-in [tid] (fn [_] 0))
          ;; rethrow the exception
          (throw e))))))

(defmacro wrap-fn [f show-tid?]
  `(let [wrapped-fn# (build-wrapped-fn (deref ~f) ~show-tid?)]
     ;; save the wrapped to orig mapping
     (swap! wrapped->orig assoc wrapped-fn# (deref ~f))
     ;; alter var's root to wrapped function
     (alter-var-root ~f (constantly wrapped-fn#))))

(defn unwrap-fn
  [v]
  (when (traced? @v)
    ;; alter the var back
    (alter-var-root v (constantly (get-orig @v)))
    ;; remove from the cache
    (swap! wrapped->orig dissoc @v)))

(defn trace
  "Tell tracer which namespace you want to trace.
  supported flags:
    :show-tid - print the thread id when functions has been called."
  [ns-name-sym & flags]
  (if ('#{tracer.core} ns-name-sym)
    (println "ns:" ns-name-sym "is forbidden to be traced!")
    (let [vars (ns-interns ns-name-sym)]
      (doseq [[var-name  var-obj] vars]
        (when (and (callable? var-obj) (not (traced? (deref var-obj))))
          (println (format "Add %s/%s to trace list." (name ns-name-sym)  var-name))
          (wrap-fn var-obj ((set flags) :show-tid)))))))

(defn untrace
  "Tell tracer to un-trace the objects(functions, macros) in the specified namespace."
  [ns-name-sym]
  (doseq [[var-name var-obj] (ns-interns ns-name-sym)]
    (when (traced? (deref var-obj))
      (println (format "Remove %s/%s from trace list." (name ns-name-sym)  var-name))
      (unwrap-fn var-obj))))
