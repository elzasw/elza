# ELZA Administration Guide

English administration guide, Sphinx sources. Replaces the Czech
"Administrátorská příručka" in elza-doc.git, which is reduced to a short
quick-install overview pointing here and to https://get.lightcomp.com/elza.

## Build

```
pip install -r requirements.txt
make clean html SPHINXOPTS="-W --keep-going"
```

or with Docker: `build-docker.bat`. It uses the image named by the
`SPHINXDOC_IMAGE` environment variable (the internal sphinxdoc image the
pipeline uses) and falls back to the public `sphinxdoc/sphinx` image.
`ELZA_DOC_VERSION` sets the version line shown in the pages (default `dev`).

Warnings are errors (`-W`): a broken reference or a page missing from a
toctree fails the build.

## Writing rules

- Write in English directly. Do not translate the Czech admin chapters
  one to one: verify every statement against the current code and
  configuration (`elza-core/src/main/resources/elza.yaml`,
  `elza-web/config/elza.yaml.template`, `@Value` / `@ConfigurationProperties`).
- UI names (menus, buttons, archival terms) must match the English UI
  catalog `elza-react/lang/translated/en.json`.
- Do not duplicate the install scripts documentation; link to
  https://get.lightcomp.com/elza instead.
- A change of a configuration key, default or behaviour described here
  updates this guide in the same commit.
- Chapters still to be written carry their scope as `.. todo::` notes.
