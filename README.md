# lein-dalap

A Leiningen plugin to transform clojure source code into
clojurescript.

## Usage

Put `[lein-dalap "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
project.clj.

### Transformation rules

In order to transform files from clojure to clojurescript you will need a `<project-root>/dalap_rules.clj' file. The format is the following:

```clojure
;; The file is going to be composed of tuples of file-specs
;; (input-paths, output-paths tuples) and transformation rules
;; (selector, transfomer tuples)

{
  ["src/clj/foo.clj" "src/cljs/foo.cljs"]
  ;; ^ input file ^ generated output file

  ;; following are the transformation rules for this
  ;; file-spec
  [
    'Object 'default
    ;; ^ you may use symbols as selectors and transformers
    ;; this will replace an `Object' symbol, `default' symbol in your
    ;; final cljs source code

    (when (has-meta? :cljs)) (transform (replace-with-meta :cljs))
    ;; ^ you may also use functions as selectors as transformers
    ;; as long as you wrap them with the `when' and `transform' functions.
    ;; This specific selector will replace your source code like:
    ;;
    ;; from clojure:
    ;; (^{:cljs '-invoke} invoke [args] ...)
    ;; To clojurescript:
    ;; (-invoke [args] ...)
    ;;
    ;; We use valid clojure syntax to annotate how we want
    ;; the clojurescript output to be.
  ]
  ;; For more information about all the different rules you can
  ;; specify check http://github.com/van-clj/dalap
}
```
### Default transformation rules

* When adding a `^:clj` meta tag to a clj form, this won't be
  translated to cljs.

  Example:

  ```clojure
    ^:clj
    (throw (IndexOutOfBoundException. "some error"))
    ;; ^ this won't be on the cljs output file
  ```

* When adding a `^{:cljs 'replacement-form}' to a clj form, this
  will be replaced with the specified replacement-form.

  Example:

  ```clojure
    (throw (new ^:cljs {'js/Error} IndexOutOfBoundException "some error"))
    ;; ^ this will be replaced with
    (throw (new js/Error "some error"))
  ```

* When adding an ignore reader macro that starts with :cljs, this
  from will be translated in a top-level form on the output cljs file.

  Example:

  ```clojure
    #_(:cljs (.log js/console "hello world")
    ;; ^ this will be replaced with

    (do (.log js/console "hello world"))

    ;; on the output cljs file
  ```

* All basic clojure types are automatically translated to clojurescript

  Example:

  ```clojure
  (extend-protocol IProtocol
    Object
    (my-fn [obj] ...))
    String
    (my-fn [str] ...))

  ;; this will be translated to:
  (extend-type IProtocol
    default
    (my-fn [obj] ...)
    string
    (my-fn [str] ...))
  ```

### Plugin execution

The plugin works the same way as [lein-cljsbuild](), you will have
three sub-commands you can excute:

* auto: To automatically compile files (specified on dalap_rules.clj)
  that has been modified
* once: To compile once files specified on dalap_rules.clj
* clean: To remove all generated cljs files

### Hooks for lein-cljsbuild

If you add `:hooks [leiningen.dalap]` in your `project.clj` file
the transformation of clj to cljs files will be executed automatically
before cljsbuild compilation.

## License

Copyright © 2012 Birdseye Software.

Distributed under the Eclipse Public License, the same as Clojure.
