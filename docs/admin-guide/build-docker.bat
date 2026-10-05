@echo off
REM ---------------------------------------------------------------------------
REM Builds the administration guide (HTML) with a Sphinx Docker image.
REM
REM The image name comes from the SPHINXDOC_IMAGE environment variable
REM (the internal sphinxdoc image used by the pipeline). Without it the
REM public sphinxdoc/sphinx image is used and the RTD theme is installed on
REM the fly (internet required).
REM
REM The repository is publicly mirrored: registry URLs and credentials never
REM belong in this script.
REM
REM ELZA_DOC_VERSION, when set, is the version line the pages name ("3.4");
REM unset, the pages say "dev". The pipeline sets it from the release branch.
REM ---------------------------------------------------------------------------

pushd %~dp0

if "%SPHINXDOC_IMAGE%" == "" goto public

echo Building documentation with %SPHINXDOC_IMAGE% ...
docker run --rm -v "%cd%":/data -w /data -e ELZA_DOC_VERSION %SPHINXDOC_IMAGE% make clean html SPHINXOPTS="-W --keep-going"
goto end

:public
echo SPHINXDOC_IMAGE not set - using the public sphinxdoc/sphinx image...
docker run --rm -v "%cd%":/data -w /data -e ELZA_DOC_VERSION sphinxdoc/sphinx:latest sh -c "pip install --quiet sphinx-rtd-theme && make clean html SPHINXOPTS='-W --keep-going'"

:end
echo Output: build\html\index.html
popd
