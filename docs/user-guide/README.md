# ELZA User Guide

English user guide, Sphinx sources. It is for archivists who describe fonds
with the international rule set ISAD(G) (package `ISADG`, module
`rules-en-isadg`) and archival entities with ISAAR(CPF) (package
`ISAAR_CPF`, module `package-isaar-cpf`). Installation and operation are in
the [administration guide](../admin-guide/README.md); preparing and
customizing rules packages is in the
[implementation guide](../implementation-guide/README.md).

The Czech rules (ZP2015, CAM) and the Czech user documentation stay in
elza-doc.git. This guide exists in English; a Czech translation can follow
the way the administration guide is translated (a comment at the top of
each translated chapter names the source file and the elza commit).

## Publishing

The CI jobs `build-doc-en` and `deploy-doc-en` (`.elza-ci.yml` in elza-build)
publish the English guides to `https://docs.lightcomp.cz/elza/en/<edition>/`;
this guide goes to `user-guide/` next to `admin-guide/` and
`implementation-guide/` once the jobs list it.

## Build

```
pip install -r requirements.txt
make clean html SPHINXOPTS="-W --keep-going"
```

or with Docker: `build-docker.bat` (see the administration guide for the
`SPHINXDOC_IMAGE` and `ELZA_DOC_VERSION` variables).

Warnings are errors (`-W`): a broken reference or a page missing from a
toctree fails the build.

## Writing rules

- Describe what the rules packages do, in the words of the archivist: the
  elements by their names in the user interface, the levels, the scenarios,
  the validation messages. Codes (`ISADG_TITLE`) appear only where the user
  meets them: in exports, in the CSV import and in the search filters.
- Verify every statement against the packages themselves: the generator
  `rules-en-isadg/generator/gen_isadg.py` (item types, specifications,
  rules, settings and the Czech translation come from its tables) and
  `package-isaar-cpf/generator/gen_isaar.py`.
- UI names (menus, buttons, archival terms) must match the English UI
  catalog `elza-react/lang/translated/en.json`.
- Chapters still to be written carry their scope as `.. todo::` notes.
- Chapter files are named by their order, `NN-name.rst`, matching the
  chapter numbers of the output. Part I (chapters 1-9) is the archival
  description with ISAD(G), part II (chapters 10-12) the entities with
  ISAAR(CPF).
