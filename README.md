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
(wrap-ns 'blind.reader)
```

* Invoke your function to see what happens( **you get a call tree & with the parameter value!** ):

```clojure
user=> (read-string "\"hello\"")
|--[TRACER] blind.reader$read-string ("hello")
  |--[TRACER] blind.reader$string-push-back-reader ("hello")
    |--[TRACER] blind.reader$string-push-back-reader ("hello" 1)
      |--[TRACER] blind.reader$string-reader ("hello")
  |--[TRACER] blind.reader$read (#<PushbackReader blind.reader.PushbackReader@1bae939f> true nil false)
    |--[TRACER] blind.reader$char (")
    |--[TRACER] blind.reader$whitespace? (")
    |--[TRACER] blind.reader$number-literal? (#<PushbackReader blind.reader.PushbackReader@1bae939f> ")
      |--[TRACER] blind.reader$numeric? (")
    |--[TRACER] blind.reader$comment-prefix? (")
    |--[TRACER] blind.reader$macros (")
    |--[TRACER] blind.reader$read-string* (#<PushbackReader blind.reader.PushbackReader@1bae939f> ")
      |--[TRACER] blind.reader$char (h)
      |--[TRACER] blind.reader$char (e)
      |--[TRACER] blind.reader$char (l)
      |--[TRACER] blind.reader$char (l)
      |--[TRACER] blind.reader$char (o)
      |--[TRACER] blind.reader$char (")
"hello"
```

## License

Copyright (C) 2013 xumingming

Distributed under the Eclipse Public License, the same as Clojure.
