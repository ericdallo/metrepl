# Changelog

## [Unreleased]

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
