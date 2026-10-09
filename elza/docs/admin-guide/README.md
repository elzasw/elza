# ELZA Administration Guide

English administration guide, Sphinx sources. This is the source version
of the guide. The Czech documentation (elza-doc.git, `source/admin`)
keeps a Czech translation of it; each Czech chapter starts with a comment
naming its source file here and the elza commit it was synchronised
with, for example:

```
.. Zdroj: elza/docs/admin-guide/source/04-configuration.rst, elza 743f2a76be
```

Changes still to be translated are listed by
`git log --follow <commit>..HEAD -- docs/admin-guide/source/<file>`; after translating,
update the commit in the comment.

## Publishing

The CI jobs `build-doc-en` and `deploy-doc-en` (`.elza-ci.yml` in elza-build)
publish the guide to `https://docs.lightcomp.cz/elza/en/<edition>/admin-guide/`,
where the edition is `3.4` for branch 3.4.x and `main` for main; `current` and
`next` point to the current release line and to main. The Czech translation is
published with the Czech documentation at `https://docs.lightcomp.cz/elza/cs/`.

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

The list of changes is one list in two languages: the Czech
`source/whatsnew/changelog.rst` in elza-doc.git and `source/release-notes/<line>.rst`
here. Both carry the same builds and entries for users and administrators;
only the Czech list has the sections of the Czech rules and code list
packages (ZP2015, CZ_BASE). Both are updated in the same release step, and
an entry requiring an administrator's action names the configuration key.

## Writing rules

- Write here first, in English; the Czech copy follows. Verify every
  statement against the current code and configuration
  (`elza-core/src/main/resources/elza.yaml`,
  `elza-web/config/elza.yaml.template`, `@Value` / `@ConfigurationProperties`).
- UI names (menus, buttons, archival terms) must match the English UI
  catalog `elza-react/lang/translated/en.json`; the Czech copy uses the
  Czech texts of the same catalog keys.
- Do not duplicate the install scripts documentation; link to
  https://get.lightcomp.com/elza instead.
- A change of a configuration key, default or behaviour described here
  updates this guide in the same commit.
- Chapters still to be written carry their scope as `.. todo::` notes.
- Chapter files are named by their order, `NN-name.rst`, matching the
  chapter numbers of the output; the Czech copy uses the same numbers.
  Moving or inserting a chapter renames the files (and changes their
  URLs) in both languages. The changes of each version line are in
  `source/release-notes/<line>.rst`.
