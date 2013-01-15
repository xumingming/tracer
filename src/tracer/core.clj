(ns tracer.core)

(defmacro wrap-fn [f]
  `(alter-var-root ~f (fn [original#]
                        (fn [& args#]
                          (println "[TRACER-ak47]" original# args#)
                          (let [ret# (apply original# args#)]
                            ret#)))))

(defn wrap-ns [ns-name-sym]
  (let [vars (ns-interns ns-name-sym)]
     (doseq [[var-name var-obj] vars
             :let [var-name (name var-name)
                   var-ns (-> var-obj meta :ns .getName name)]]
      (if-not (= "log" var-name)
        (wrap-fn var-obj)))))

