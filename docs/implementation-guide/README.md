# ELZA Implementation Guide

English implementation guide, Sphinx sources. It is for implementers who
prepare rules packages, customize the rules of an installation and connect
ELZA to other systems. Installation and operation are in the
[administration guide](../admin-guide/README.md).

This guide exists in English only; unlike the administration guide it has
no Czech translation. The Czech documentation (elza-doc.git,
`source/implementation`, "Průvodce implementací") points to it in its
chapter "Vytváření balíčků pravidel". The former Czech description of the
package files was merged into chapter 2 here and removed there.

## Publishing

The CI jobs `build-doc-en` and `deploy-doc-en` (`.elza-ci.yml` in elza-build)
publish the guide together with the administration guide to
`https://docs.lightcomp.cz/elza/en/<edition>/implementation-guide/`, where the
edition is `3.4` for branch 3.4.x and `main` for main; `current` and `next` point
to the current release line and to main. The Czech documentation is at
`https://docs.lightcomp.cz/elza/cs/`.

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

- Verify every statement against the current code: the package import
  (`elza-core/src/main/java/cz/tacr/elza/packageimport/`), the XML classes in
  its `xml` subpackage, and the rules engine (`elza-core/.../drools/`).
- A change of the package format or of rule behaviour updates this guide in
  the same commit.
- UI names must match the English UI catalog
  `elza-react/lang/translated/en.json`.
- Examples use the test packages under `elza-core/src/test/resources/`
  where possible, so the examples are known to import.
- Chapters still to be written carry their scope as `.. todo::` notes.
- Chapter files are named by their order, `NN-name.rst`.
