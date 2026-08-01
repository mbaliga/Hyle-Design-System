# Contributing

## License of your contribution

This project is licensed under **Apache-2.0** (see `LICENSE`; fonts under `fonts/*` are OFL —
see `NOTICE`). By submitting a contribution to the Apache-2.0-covered parts of this repo, you
agree it's licensed under the same terms (Apache-2.0 §5 covers this — no separate CLA).

## Developer Certificate of Origin (DCO)

Sign off every commit, certifying you wrote it or otherwise have the right to submit it — the
[Developer Certificate of Origin](https://developercertificate.org/). Use `git commit -s`, or add
by hand:

```
Signed-off-by: Your Name <your.email@example.com>
```

## Before opening a PR

- `dev.aarso:hyle` is consumed by multiple sibling repos (`android-ide-core`,
  `Android-IDE-Studio`) via git submodule + Gradle `includeBuild` — a breaking change here has
  ripple effects. Flag breaking changes clearly in the PR description.
- `0.1.0` of `dev.aarso:hyle` is permanently burned (three divergent definitions once shipped
  under it) — never reuse that version string.
- New fonts or font modifications: OFL requires a distinct family/Reserved-Font-Name if you're
  forking an existing Hyle family under a new identity — see `TRADEMARKS.md`.
- Run `./gradlew :hyle:test` (and `:crash-recovery`'s own tests, if touched) before opening a PR
  if you have the tooling; note in the PR if you couldn't.

## Trademark note

See `TRADEMARKS.md` — the code/font licenses don't carry trademark or family-name permission on
their own.
