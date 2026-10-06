=============
Package Files
=============

Reference of the files of a rules package. Paths are relative to the root
of the package archive; ``<RS>`` is the code of a rule set. Elements are
optional unless marked *required*. A package leaves out the files it does
not need; each file it contains needs at least its root element.

The full set of elements is defined by the classes in
``cz.tacr.elza.packageimport.xml`` of the ELZA sources; the packages
``package-cz-base``, ``rules-cz-zp2015`` and ``rules-simple-dev`` in the
sources are complete examples.

package.xml
===========

.. code-block:: xml

   <package>
     <code>ADDON_TEST</code>
     <name>Addon test package</name>
     <version>2</version>
     <description>...</description>
     <language>en</language>
     <dependencies>
       <dependency code="ZP2015" min-version="344"/>
     </dependencies>
   </package>

``code`` (required)
   Code of the package, unique in the installation.

``name`` (required)
   Name shown in the list of packages.

``version`` (required)
   Integer; must grow with every change (see :doc:`01-rules-packages`).

``description``
   Free text.

``language``
   BCP 47 tag of the language the package writes its texts in (names,
   shortcuts and descriptions of its entities, messages); ``cs`` when the
   element is missing. Texts in other languages come from translation files
   (see :ref:`translation-files`). The language must be known to the
   installation (table ``sys_language``), otherwise the import is refused.

``dependencies/dependency``
   ``code`` and ``min-version`` (both required) of a package this package
   builds on.

rul_item_type.xml
=================

Item types (elements of description). They are offered in the order of the
file, after the item types of the packages imported earlier.

.. code-block:: xml

   <item-types>
       <item-type code="ADT_STAGE" data-type="STRING">
           <name>Addon stage</name>
           <shortcut>Stage</shortcut>
           <description>...</description>
           <is-value-unique>false</is-value-unique>
           <can-be-ordered>false</can-be-ordered>
           <use-specification>false</use-specification>
       </item-type>
   </item-types>

``code`` (required, attribute)
   Code of the item type, unique across all packages.

``data-type`` (required, attribute)
   ``STRING`` (one line), ``TEXT``, ``FORMATTED_TEXT``, ``INT``,
   ``DECIMAL``, ``DATE``, ``UNITDATE`` (archival dating), ``UNITID``
   (reference code), ``ENUM`` (specification only), ``RECORD_REF``
   (archival entity), ``STRUCTURED`` (structured object), ``JSON_TABLE``,
   ``COORDINATES``, ``FILE_REF`` (attachment), ``URI_REF`` (link), ``BIT``.

``structure-type`` (attribute)
   For ``STRUCTURED``: code of a structured type of the same package.

``name``, ``shortcut`` (required)
   Name in forms and its short form.

``description``
   Help text shown to archivists.

``use-specification`` (required)
   Whether values carry a specification from :file:`rul_item_spec.xml`.
   An ``ENUM`` value consists of the specification only, so ``ENUM`` item
   types set it to ``true``. It cannot be switched while values exist.

``is-value-unique``, ``can-be-ordered``
   Flags passed to clients (``can-be-ordered`` is the ``orderable`` flag of
   the item type dictionary of the REST API). Default ``false``. ELZA does
   not enforce uniqueness.

``string-length-limit``
   For ``STRING``: maximal length.

``mask``
   For ``STRING``: display mask, for example ``### ### *``.

``display-type``
   For ``INT``: ``NUMBER`` (default) or ``DURATION`` (shown as ``HH:mm:ss``).

``columns-definitions/column``
   For ``JSON_TABLE``: columns with attributes ``code`` and ``data-type``
   and elements ``name`` and ``width``.

``item-aptypes/item-aptype``
   For ``RECORD_REF``: attribute ``register-type`` with the code of an
   entity type that may be referenced; repeat for more types.

The data type of an existing item type can be changed only while no values
exist, with two exceptions that convert existing values: ``STRING`` to
``DATE`` and ``TEXT`` to ``STRING``.

rul_item_spec.xml
=================

Specifications: the values of ``ENUM`` item types and the qualifiers of
other item types with ``use-specification``.

.. code-block:: xml

   <item-specs>
       <item-spec code="ZP2015_OTHERID_SIG">
           <name>signatura</name>
           <description>...</description>
           <shortcut>sign.</shortcut>
           <item-type-assign code="ZP2015_OTHER_ID"/>
       </item-spec>
   </item-specs>

``code`` (required, attribute)
   Code of the specification, unique across all packages.

``name``, ``description``, ``shortcut`` (required)
   Texts shown to archivists.

``item-type-assign``
   Item type the specification belongs to; repeat it to share the
   specification among several item types. Attributes ``code`` (required;
   the item type may belong to another package) and ``view-after`` (code of
   the specification of the same item type after which this one is placed).
   A specification without any assignment is not offered anywhere.

``categories/category``
   Categories grouping long lists of specifications in the selection.

``item-aptypes/item-aptype``
   For specifications of ``RECORD_REF`` item types: entity types that may be
   referenced with this specification.

rul_rule_set.xml
================

Rule sets owned by the package.

.. code-block:: xml

   <rule-sets>
       <rule-set code="ZP2015">
           <name>Základní pravidla pro zpracování archiválií</name>
           <rule-type>ARRANGEMENT</rule-type>
           <rule-item-type-filter>ItemTypeFilter.drl</rule-item-type-filter>
       </rule-set>
   </rule-sets>

``code`` (required, attribute), ``name`` (required)
   Code and name of the rule set; the name is offered when a fund is
   created.

``rule-type`` (required)
   ``ARRANGEMENT`` for the description of funds, ``ENTITY`` for archival
   entities.

``rule-item-type-filter``
   Drools file in :file:`rul_rule_set/<RS>/rules/` deciding which item
   types the rule set uses.

``compatibility-rul-package``
   A package version: when the package is upgraded from a lower version,
   all funds described under the rule set are validated again.

rul_rule_set/<RS>/rul_arrangement_rule.xml
==========================================

Rule files of the rule set, in :file:`rul_rule_set/<RS>/rules/`.

.. code-block:: xml

   <arrangement-rules>
       <arrangement-rule filename="AvailableItems.drl">
           <rule-type>ATTRIBUTE_TYPES</rule-type>
           <priority>100</priority>
       </arrangement-rule>
   </arrangement-rules>

``filename`` (required, attribute), ``rule-type`` (required), ``priority`` (required)
   Rule types for archival description are described in
   :doc:`01-rules-packages`. Further types: ``PLAIN_TEXT_GENERATOR``
   (Groovy script building the citation of a unit of description),
   ``DA_IMPORT`` and ``DA_MATCH`` (Groovy scripts for packages from a
   digital archive), and for the entity rule set ``AP_MAPPING_TYPE`` and
   ``AUTO_ITEMS``.

rul_rule_set/<RS>/rul_arrangement_extension.xml and rul_extension_rule.xml
==========================================================================

Arrangement extensions are optional sets of rules that archivists switch on
for a unit of description and the units below it.

.. code-block:: xml

   <arrangement-extensions>
       <arrangement-extension code="ZP2015_DilciListy">
           <name>Dílčí listy NAD</name>
       </arrangement-extension>
   </arrangement-extensions>

   <extension-rules>
       <extension-rule filename="DilciListy.drl" arrangement-extension="ZP2015_DilciListy">
           <rule-type>ATTRIBUTE_TYPES</rule-type>
           <priority>100</priority>
       </extension-rule>
   </extension-rules>

``arrangement-extension``: ``code`` (required, attribute, unique across
packages) and ``name`` (required). ``extension-rule``: ``filename`` and
``arrangement-extension`` (required, attributes), ``rule-type``
(``ATTRIBUTE_TYPES``, ``CONFORMITY_INFO``, ``CONFORMITY_IMPACT``,
``NEW_LEVEL``) and ``priority`` (required); ``compatibility-rul-package``
(attribute) as for rule sets; ``condition`` is used by the entity rule set.

rul_rule_set/<RS>/rul_policy_type.xml
=====================================

Policy types group validation results; archivists can hide the results of
a policy type in *Validation rules*. Rules name them in
``setPolicyTypeCode`` and in validation results.

.. code-block:: xml

   <policy-types>
       <policy-type code="ZP2015_POL_BASIC">
           <name>Základní</name>
       </policy-type>
   </policy-types>

rul_rule_set/<RS>/rul_output_type.xml and rul_template.xml
==========================================================

Output types (finding aids, lists, labels) and the templates that produce
them.

.. code-block:: xml

   <output-types>
       <output-type code="ZP2015_INVENTAR" filename="Output_Inventar.drl">
           <name>Archivní pomůcka (inventář, katalog)</name>
       </output-type>
   </output-types>

   <templates>
       <template code="ZP2015_INVENTAR_PDF_JASPER" output-type="ZP2015_INVENTAR"
                 mime-type="application/pdf" extension="pdf">
           <name>PDF</name>
           <engine>JASPER</engine>
           <directory>POMUCKA_PDF_JASPER</directory>
       </template>
   </templates>

``output-type``: ``code`` and ``name`` (required); ``filename``: Drools
file in :file:`rules/` deciding the item types of the output.

``template``: ``code``, ``output-type``, ``mime-type`` and ``extension``
(required, attributes); ``name``, ``engine`` (``JASPER``, ``FREEMARKER``,
``DOCX``, ``DE_XML``) and ``directory`` (required; a directory in
:file:`rul_rule_set/<RS>/templates/`); ``validation-schema`` (namespace of
an XML schema the output is validated against); ``other-codes`` (former
codes of the template, so outputs created with them keep working).

rul_rule_set/<RS>/rul_package_actions.xml
=========================================

Bulk actions, defined in YAML files in :file:`rul_rule_set/<RS>/bulk_actions/`.

.. code-block:: xml

   <package-actions>
       <package-action filename="ZP2015_GENERATOR_UNIT_ID.yaml">
           <action-recommendeds>
               <action-recommended output-type="ZP2015_INVENTAR"/>
           </action-recommendeds>
       </package-action>
       <package-action filename="ZP2015_INTRO.yaml">
           <action-item-types>
               <action-item-type item-type="ZP2015_UNIT_SOURCE"/>
           </action-item-types>
       </package-action>
   </package-actions>

``filename`` (required, attribute); ``action-recommendeds`` - output types
for which the action is recommended before the output is generated;
``action-item-types`` - item types the action fills.

rul_structure_type.xml and rul_structure_definition.xml
=======================================================

Structured types (for example storage units) and the scripts that define
them.

.. code-block:: xml

   <structure-types>
       <structure-type code="ZP2015_PACKET">
           <name>Uložení</name>
       </structure-type>
   </structure-types>

   <structure-definitions>
       <structure-definition structure-type="ZP2015_PACKET" filename="Packet.drl">
           <def-type>ATTRIBUTE_TYPES</def-type>
           <priority>100</priority>
       </structure-definition>
   </structure-definitions>

``structure-type``: ``code`` and ``name`` (required); ``anonymous`` - the
objects belong to a single item and are edited in place, they cannot be
set in bulk; ``validValueFromVersion`` - a package version: when the
package is upgraded from a lower version, the values of all objects of the
type are generated again.

``structure-definition``: ``structure-type``, ``filename`` (required,
attributes), ``def-type`` (required) and ``priority`` (required):
``ATTRIBUTE_TYPES`` is a Drools file in :file:`rules/` deciding the items
of the object; ``SERIALIZED_VALUE`` and ``PARSE_VALUE`` are Groovy scripts
in :file:`scripts/` producing the text value of the object and parsing
it back. ``compatibility-rul-package`` (attribute) - a package version:
when the package is upgraded from a lower version, archival entities whose
parts use the structured type are queued for regeneration.

Structure extensions (:file:`rul_structure_extension.xml`,
:file:`rul_structure_extension_definition.xml`) add optional definitions
to a structured type.

.. _translation-files:

translations/<lang>.xml
=======================

Texts of a package in another language - of its own entities, or of the
entities of any other package. One file per language, named by its BCP 47
tag (:file:`translations/en.xml`), so a translator works with one file.

.. code-block:: xml

   <translations lang="en">
     <t type="ITEM_TYPE" code="SRD_TITLE" field="name">Content, abstract</t>
     <t type="ITEM_SPEC" code="SRD_LEVEL_SERIES" field="shortcut">Series</t>
     <t type="MESSAGE" code="ADDON_TEST/ADT_001" field="text">Stage {0} is missing.</t>
   </translations>

``lang`` (required, attribute)
   Tag of the language, equal to the file name. Tags are case-insensitive
   (``en-GB`` = ``en-gb``); a package has at most one file per language.

``t``
   One translated text. ``type`` and ``field`` say which text of which kind
   of entity, ``code`` is the code of the entity (at most 100 characters),
   the element text is the translation and must not be empty:

   ========================= ==================================== ===========================
   ``type``                  ``code``                             ``field``
   ========================= ==================================== ===========================
   ``ITEM_TYPE``             item type                            ``name``, ``shortcut``,
                                                                  ``description``
   ``ITEM_SPEC``             specification                        ``name``, ``shortcut``,
                                                                  ``description``
   ``RULE_SET``              rule set                             ``name``
   ``AP_TYPE``               entity type                          ``name``
   ``PART_TYPE``             part type                            ``name``
   ``STRUCTURED_TYPE``       structured type                      ``name``
   ``POLICY_TYPE``           policy type                          ``name``
   ``OUTPUT_TYPE``           output type                          ``name``
   ``TEMPLATE``              output template                      ``name``
   ``ARRANGEMENT_EXTENSION`` arrangement extension                ``name``
   ``ISSUE_TYPE``            issue type                           ``name``
   ``ISSUE_STATE``           issue state                          ``name``
   ``TYPE_GROUP``            ``<RULE_SET>/<GROUP>``               ``name``
   ``MESSAGE``               ``<PACKAGE>/<KEY>``                  ``text``
   ========================= ==================================== ===========================

The text stored with the entity is the source text, in the language of
the package that defines it (``language`` in :file:`package.xml`); it is
shown when no translation into the reader's language exists. A
translation into ``en-GB`` falls back to ``en`` and then to the source
text.

**Messages.** A message is defined by the translation file of its
package's own language: ``ZP2015`` writing in Czech defines
``ZP2015/UJ_012`` in :file:`translations/cs.xml`, and any package can
translate it in a file of another language. The code starts with the code
of the defining package. Arguments are ``java.text.MessageFormat``
placeholders (``{0}``, ``{1,number,integer}``), formatted with the
conventions of the language; an apostrophe is written ``''``. The import
compiles every message and refuses a broken pattern.

**The package's own language.** Texts of the package's own entities are in
the entity files, so in the file of its own language rows translating them
are skipped with a warning. Rows for entities of other packages, and
messages of other packages, are imported: a customization written in Czech
renames a Czech text of the package it builds on this way.

**Several packages, one text.** When several packages translate the same
text into the same language, the package that depends on the other wins,
so a customization overrides the translations of the package it builds
on. Packages without a dependency between them are ordered by code.

**Import.** Each import replaces all translations of the package. It is
refused - with the file, the row and the reason in the error - for an
unknown ``type``, a missing ``code``, a ``field`` missing or not allowed
for the type, an empty text, an unknown language, two files of one
language, the same text twice in one file, a message or type-group code
without its prefix, or a message that is not a valid pattern. A
translation of an entity that does not exist is kept with a warning: the
entity may come with the next version of its package. The translations
are exported with the package and removed with it.

**Outdated translations.** Each translation carries a hash of the source
text it was made from, optionally written in the file:

.. code-block:: xml

   <t type="ITEM_TYPE" code="SRD_TITLE" field="name" src-hash="3f9a1c07b2e4d856">Content, abstract</t>

The import takes ``src-hash`` from the file when present. Without it, a
translation whose text did not change since the previous import keeps the
hash it had, and a new or changed translation gets the hash of the current
source text. The export writes the hash, so an exported package keeps it.
When the source text changes later (a new version of the package that
defines the entity), the translation is reported as outdated - also after
the translating package is imported again unchanged; it is still used
until the translator updates it. Translations of type groups are not
checked.

.. todo::

   Remaining files: :file:`ui_setting.xml` (package and rule set level),
   :file:`ap_type.xml`, :file:`rul_part_type.xml`,
   :file:`ap_external_id_type.xml`, :file:`par_institution_type.xml`, issue
   types and states, export and output filters, import transformations.
