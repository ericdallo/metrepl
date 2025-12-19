# Changelog

## Unreleased

## 0.5.0

- [#1](https://github.com/ericdallo/metrepl/pull/1) Enable exporting metrics from `otlp` exporter. 

## 0.4.2

- Bump nrepl to 1.5.0

## 0.4.1

- Bump opentelemetry to 1.51.0.

## 0.4.0

- Replace `event/first-op-requested` with `info/repl-ready` adding more info about the project.
- Add `:first-time` to `:event/op-completed` and `:event/op-requested` when op is `load-file` and first time processing it.

## 0.3.4

- Fix OpenTelemetry integration race condition corner case.

## 0.3.3

- Fix read config from classpath

## 0.3.2

- Add compatibility with older opentelemetry versions.

## 0.3.1

- Add compatibility with older Clojure versions

## 0.3.0

- Add `event/test-passed`, `event/test-errored` and `event/test-failed` events.
- Add `session-time-ms` to `close` op.
- Add `:project-path` to metrics.

## 0.2.0

- Improve export exception handler
- Remove jvm started flaky metric
- Fix `event/op-completed` metric to measure time correctly
- Add `event/test-executed` event.

## 0.1.2

- First release
