# lein-dalap [![Build Status](https://secure.travis-ci.org/BirdseyeSoftware/lein-dalap.png?branch=master)](https://travis-ci.org/BirdseyeSoftware/lein-dalap)

A Leiningen plugin to transform clojure source code into
clojurescript.

## Usage

Put `[com.birdseye-sw/lein-dalap "0.1.0"]` into the `:plugins` vector of your
project.clj.

## Documentation & Examples

Please refer to our [documentation site][documentation_site].

### Transformation rules

In order to transform files from clojure to clojurescript you will
need a `<project-root>/dalap_rules.clj' file. The following is an
example of the rules format:

```clojure
{
  ;; each entry is a tuple of [input file, output file] ...
  ["src/clj/foo.clj" "src/cljs/foo.cljs"] 
  ;; ... followed by the transformation rules to use. 
  ;; Rules are pairs of selector + transformer
  [
    'java.lang.Object 
    'default
    ;; ^ you may use symbols as selectors and transformers
    ;; this will replace `java.lang.Object' with `default' in your
    ;; final cljs source code.

    (dalap/when (has-meta? :cljs)) (dalap/transform (replace-with-meta :cljs))
    ;; ^ you may also use functions as selectors and transformers
    ;; Wrap selector functions in `dalap/when' and transformer
    ;; functions in `dalap/transform'.
    ;; This specific selector will replace your source code like:
    ;;
    ;; from clojure:
    ;; (^{:cljs '-invoke} invoke [args] ...)
    ;; To clojurescript:
    ;; (-invoke [args] ...)
    ;;
  ]
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
    java.lang.Object
    (my-fn [obj] ...))
    java.lang.String
    (my-fn [str] ...))

  ;; this will be translated to:
  (extend-type IProtocol
    default
    (my-fn [obj] ...)
    string
    (my-fn [str] ...))
  ```

### Plugin execution

The plugin works the same way as
[lein-cljsbuild](https://github.com/emezeske/lein-cljsbuild), you will
have three sub-commands you can excute:

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

Distributed under the MIT License.

[documentation_site]:http://birdseye-sw.com/oss/lein-dalap/
