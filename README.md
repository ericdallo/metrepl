[![Clojars](https://img.shields.io/clojars/v/dev.ericdallo/metrepl.svg)](https://clojars.org/dev.ericdallo/metrepl)

# metrepl

_Metrics of your Clojure nREPL_

## What

Metrepl is a [nREPL middleware](https://nrepl.org/nrepl/design/middleware.html) that get metrics about your REPL usage (startup time, ops, eval, load-file, errors, client info and more) and export to multiple configurable places. 
This is useful to have metrics about the REPL usage and understand how users are using its features, if they are facing a specific problem, slowness.

## Concepts

### Metrics

Metrepl follows a standard of `<type>/<metrics-id>`, example: `event/op-completed` and each metric may have additional info in its `payload`,  besides that all metrics have information about the os, hostname and unique id for the nREPL session.

For all available metrics, check [here](./docs/all-metrics.edn).

### Exporters

- `stdout`: Export the metric to current nREPL process stdout, useful for debugging.
- `file`: Export the metric to a file, appending each metric in a new line.
- `otlp`: Export the metric via [OpenTelemetry](https://opentelemetry.io/) to what user configured.

For all available exporters and their configs, check [here](./docs/all-exporters.edn).

### Configuration

Metrepl supports a advancded configuration via the following waterfall, merging from top to bottom:

1. base: fixed config var `metrepl.config/initial-config`.
2. classpath: searching for a `metrepl.exports/config.edn` file in the current classpath.
3. env var: searching for a `METREPL_CONFIG` env var which should contains a valid edn config.
4. config-file: searching from a local `.metrepl.edn` file.
5. dynamic-var: the dynamic value in `metrepl.config/*config*`.

Check all available configuration options in [metrepl.config](https://github.com/ericdallo/metrepl/blob/master/src/metrepl/config.clj#L14).

## How to use

1. Add the `metrepl/middleware` middleware to your repl, there are multiple ways to achieve that depending on `lein` or `deps.edn` projects for example, example of starting a REPL with metrepl middleware:

```sh
clojure -Sdeps "{:deps {nrepl/nrepl {:mvn/version \"1.3.1\"} dev.ericdallo/metrepl {:mvn/version \"x.y.z\"}} :aliases {:start/nrepl {:main-opts [\"-m\" \"nrepl.cmdline\" \"--middleware\" \"[metrepl/middleware]\"]}}}" -M:start/nrepl
```

2. Configure to where export your metrics, example: `METREPL_CONFIG={:exporters {:file {:enabled? true :path "/tmp/foo.txt"}}}`

# Support 

Consider support the work of this project [here](https://github.com/sponsors/ericdallo) ❤️
