# tracer

trace clojure call stack.

When you read some clojure source code, you may find some function/logic very complex, it is not so simple to understand the whole logic. I found the same issue, so I developed this simple tool to facilitate reading of clojure source code.
## Usage

* Include `tracer` in your `project.clj`

```clojure
[tracer "1.0.0-SNAPSHOT"]
```

* Go into your project home and start the repl:

```bash
lein repl
```

* `use` the `tracer.core` namespace

```clojure
(use 'tracer.core)
```

* Tell `tracer` which namespace you want to trace:

```clojure
;; CHANGE 'blind.reader TO YOUR OWN namespace
(trace 'blind.reader)
```

* Invoke your function to see what happens( **you get a call tree & with the parameter value!** ):

```clojure
user=> (read-string "\"hello\"")
|--blind.reader$read-string ("hello")
  |--blind.reader$string-push-back-reader ("hello")
    |--blind.reader$string-push-back-reader ("hello" 1)
      |--blind.reader$string-reader ("hello")
  |--blind.reader$read (#<PushbackReader blind.reader.PushbackReader@3eae3da8> true nil false)
    |--blind.reader$char (")
    |--blind.reader$whitespace? (")
    |--blind.reader$number-literal? (#<PushbackReader blind.reader.PushbackReader@3eae3da8> ")
      |--blind.reader$numeric? (")
    |--blind.reader$comment-prefix? (")
    |--blind.reader$macros (")
    |--blind.reader$read-string* (#<PushbackReader blind.reader.PushbackReader@3eae3da8> ")
      |--blind.reader$char (h)
      |--blind.reader$char (e)
      |--blind.reader$char (l)
      |--blind.reader$char (l)
      |--blind.reader$char (o)
      |--blind.reader$char (")
"hello"
```

## License

Copyright (C) 2013 xumingming

Distributed under the Eclipse Public License, the same as Clojure.
