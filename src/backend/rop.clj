(ns backend.rop)

;; https://medium.com/appsflyerengineering/railway-oriented-programming-clojure-and-exception-handling-why-and-how-89d75cc94c58

(defn apply-or-error [f [val err]]
  (if err
    [val err]
    (f val)))

(defmacro =>>
  "Threading macro, will execute a given set of functions one by one.
  If one of the functions fails, it will skip the rest of the functions and will return a fail cause."
  [val & funcs]
  (let [steps (for [f funcs] `(apply-or-error ~f))]
    `(->> [~val nil]
          ~@steps)))
