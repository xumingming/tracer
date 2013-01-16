# tracer

trace clojure call stack.

When I read some open source clojure source code, I find that some function/logic are so complex that it is not so easy to understand it, so I think it will help me a lot if I can see the actual function call stack with the provided paremeters, so I developed this simple tool to facilitate reading of clojure source code.

## Usage

* Include `tracer` in your `project.clj`

```clojure
[tracer "1.0.0-SNAPSHOT"]
```

* Go into your project home and start the repl:

```bash
lein deps && lein repl
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
user> (read-string "hello")
|-+ (blind.reader/read-string "hello")
  |-+ (blind.reader/string-push-back-reader "hello")
    |-+ (blind.reader/string-push-back-reader "hello" 1)
      |-+ (blind.reader/string-reader "hello")
  |-+ (blind.reader/read #<PushbackReader blind.reader.PushbackReader@6f54c08a> true nil false)
    |-+ (blind.reader/char \h)
    |-+ (blind.reader/whitespace? \h)
    |-+ (blind.reader/number-literal? #<PushbackReader blind.reader.PushbackReader@6f54c08a> \h)
      |-+ (blind.reader/numeric? \h)
    |-+ (blind.reader/comment-prefix? \h)
    |-+ (blind.reader/macros \h)
    |-+ (blind.reader/read-token #<PushbackReader blind.reader.PushbackReader@6f54c08a> \h)
      |-+ (blind.reader/char \e)
      |-+ (blind.reader/whitespace? \e)
      |-+ (blind.reader/macro-terminating? \e)
        |-+ (blind.reader/macros \e)
      |-+ (blind.reader/char \e)
      |-+ (blind.reader/char \l)
      |-+ (blind.reader/whitespace? \l)
      |-+ (blind.reader/macro-terminating? \l)
        |-+ (blind.reader/macros \l)
      |-+ (blind.reader/char \l)
      |-+ (blind.reader/char \l)
      |-+ (blind.reader/whitespace? \l)
      |-+ (blind.reader/macro-terminating? \l)
        |-+ (blind.reader/macros \l)
      |-+ (blind.reader/char \l)
      |-+ (blind.reader/char \o)
      |-+ (blind.reader/whitespace? \o)
      |-+ (blind.reader/macro-terminating? \o)
        |-+ (blind.reader/macros \o)
      |-+ (blind.reader/char \o)
      |-+ (blind.reader/char nil)
    |-+ (blind.reader/parse-symbol "hello")
hello
```

* Finally, you can undo all the tracing by calling `untrace` with the
same namespace symbol.

* If you want to see the thread id which the functions has been called, you can add an `:show-tid` flag when calling `trace`.

```clojure
user=>(trace 'user :show-tid)
user=>(foo 5)
18:  |-+ (user/foo 5)
18:    |-+ (user/bar 5)
```

## Contributors
* James Xu [xumingming](https://github.com/xumingming)
* Baishampayan Ghose [ghoseb](https://github.com/ghoseb)
* Ruiyun Wen [ruiyun](https://github.com/Ruiyun)

## License

Copyright (C) 2013 xumingming

Distributed under the Eclipse Public License, the same as Clojure.
