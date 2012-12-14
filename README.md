# lein-dalap [![Build Status](https://secure.travis-ci.org/BirdseyeSoftware/lein-dalap.png?branch=master)](https://travis-ci.org/BirdseyeSoftware/lein-dalap)

A Leiningen plugin to transform clojure source code into
clojurescript.

lein-dalap allows you to author code that works in both the JVM and in
the browser, without forking your code and without relying on
cljsbuild crossovers.

lein-dalap is inspired by [cljx](https://github.com/lynaghk/cljx), a
leiningen plugin that transforms input source files with a .cljx
extension and special meta-data markup into .clj and .cljs output. In
contrast with cljx, lein-dalap's input files are plain .clj files and
only the .cljs files are auto-generated. It is also simpler to specify
custom transformation rules at the project level.

The name dalap is the acronym for _Decide As Late As Possible_ from lean programming.

## Documentation & Examples

Please refer to our [documentation site][documentation_site], there is also an [example application][example].

## License

Copyright © 2012 Birdseye Software.

Distributed under the MIT License.

[documentation_site]:http://birdseye-sw.com/oss/lein-dalap/
[example]:http://github.com/BirdseyeSoftware/lein-dalap/tree/master/examples/hello_world
