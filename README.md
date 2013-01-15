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
user=> (read-string "\u0061")
|--[TRACER] blind.reader$read-string (a)
  |--[TRACER] blind.reader$string-push-back-reader (a)
    |--[TRACER] blind.reader$string-push-back-reader (a 1)
      |--[TRACER] blind.reader$string-reader (a)
  |--[TRACER] blind.reader$read (#<PushbackReader blind.reader.PushbackReader@648730b8> true nil false)
    |--[TRACER] blind.reader$char (a)
    |--[TRACER] blind.reader$whitespace? (a)
    |--[TRACER] blind.reader$number-literal? (#<PushbackReader blind.reader.PushbackReader@648730b8> a)
      |--[TRACER] blind.reader$numeric? (a)
    |--[TRACER] blind.reader$comment-prefix? (a)
    |--[TRACER] blind.reader$macros (a)
    |--[TRACER] blind.reader$read-symbol (#<PushbackReader blind.reader.PushbackReader@648730b8> a)
      |--[TRACER] blind.reader$read-token (#<PushbackReader blind.reader.PushbackReader@648730b8> a)
        |--[TRACER] blind.reader$char (nil)
      |--[TRACER] blind.reader$parse-symbol (a)
```

## License

Copyright (C) 2013 xumingming

Distributed under the Eclipse Public License, the same as Clojure.
