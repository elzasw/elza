# Sphinx configuration for the ELZA Administration Guide (English).
#
# The guide lives next to the code so that a change of configuration,
# deployment or integration behaviour updates the documentation in the
# same commit. Czech user documentation and methodics stay in elza-doc.git.

import os

project = 'ELZA Administration Guide'
copyright = '2016-2026, LightComp v.o.s.'
author = 'LightComp v.o.s.'
# Version line ("3.4") set by the pipeline from the release branch;
# the repository does not store it, the pom is the source of truth.
version = os.environ.get('ELZA_DOC_VERSION', 'dev')
release = version

extensions = [
    'sphinx.ext.intersphinx',
    'sphinx.ext.todo',
]

language = 'en'
exclude_patterns = []

# Stubs carry the scope of unfinished chapters as todo notes.
# Switch off for published builds once the chapters are written.
todo_include_todos = True

html_theme = 'sphinx_rtd_theme'
html_title = f'ELZA {version}'

# Links to the Czech documentation (rules, methodics, user guide).
# intersphinx_mapping = {
#     'cs': (f'https://docs.lightcomp.cz/elza/cs/{version}/', None),
# }

latex_documents = [
    ('index', 'elza-admin-guide.tex', 'ELZA Administration Guide',
     'LightComp v.o.s.', 'manual'),
]
latex_elements = {'papersize': 'a4paper', 'pointsize': '11pt'}
