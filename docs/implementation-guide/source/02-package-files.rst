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
           <can-be-ordered>false</can-be-ordered>
           <use-specification>false</use-specification>
       </item-type>
   </item-types>

``code`` (required, attribute)
   Code of the item type; another package may declare the same code (see
   below).

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

``can-be-ordered``
   Flag passed to clients (the ``orderable`` flag of the item type
   dictionary of the REST API). Default ``false``. The former
   ``is-value-unique`` is ignored.

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
``DATE`` and ``TEXT`` to ``STRING``. An item type is removed with its
package version only while no description or entity uses it and no
specification of another package is assigned to it.

**Item types shared by packages.** Several packages may declare an item type
with the same code - for example an international entity description
declaring ``NOTE`` or ``NM_MAIN`` of CZ_BASE. The item type then exists
once: values, rules, search and exchange use it whatever package describes
the entity. Declaring a code declared by another package asserts that it is
the same element.

- The values deciding how data are stored must agree: ``data-type``,
  ``use-specification``, ``structure-type`` and the codes and data types of
  ``columns-definitions``; a declaration that differs refuses the import.
  The owner cannot change them either while another package declares the
  item type.
- The package that created the item type owns it; its place among the item
  types and the other values (``string-length-limit``, ``mask``,
  ``display-type``, ``can-be-ordered``) are those of the owner. A declaration
  of another package takes no place in the order and may not state
  ``item-aptypes``.
- Each package states its own ``name``, ``shortcut`` and ``description`` in
  its language; the texts resolve by language as the names of classes.
- When the owner no longer declares the item type, the package winning by
  dependency order becomes the owner; the item type is removed with its
  last declaration.
- The export writes the package's declarations: its own item types in their
  order, then the declarations of item types of other packages.

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
   Code of the specification; another package may declare the same code
   (see below).

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

**Specifications shared by packages.** Several packages may declare a
specification with the same code - for example an international entity
description declaring the name type ``NT_PSEUDONYM`` of CZ_BASE. The
specification then exists once, and stored values use it whatever package
describes the entity.

- Each package states its own ``name``, ``shortcut``, ``description`` and
  ``categories``; the texts resolve by language as the names of classes.
- Each package states the item types it assigns the specification to; the
  specification belongs to every item type any declaration assigns it to.
  A package may assign its own or a shared specification to its own or a
  shared item type.
- The package that created the specification owns it: its place among the
  specifications of an item type follows the owner's order and
  ``view-after``; the category is the owner's. A declaration of another
  package may not state ``item-aptypes``.
- A specification is removed with its last declaration, an assignment with
  the last declaration assigning it; both only while no description or
  entity uses them.
- The export writes the package's declarations, each specification once
  with its assignments: its own specifications first, then the
  declarations of specifications of other packages.

ap_type.xml
===========

Entity classes (types of archival entities) the package declares.

.. code-block:: xml

   <ap-types>
       <ap-type code="PERSON">
           <name>osoba / bytost</name>
           <hierarchical>false</hierarchical>
           <read-only>true</read-only>
       </ap-type>
       <ap-type code="PERSON_INDIVIDUAL" parent-ap-type="PERSON">
           <name>fyzická osoba</name>
           <hierarchical>false</hierarchical>
           <read-only>false</read-only>
       </ap-type>
   </ap-types>

``code`` (required, attribute), ``parent-ap-type`` (attribute)
   Code of the class and of its parent; the parent is declared in the same
   file or by a package this one depends on.

``name`` (required)
   Name of the class in the language of the package.

``read-only``
   The class cannot be chosen for an entity (abstract class); the default
   of rule sets of this package that do not list their classes
   (:file:`rul_ap_type.xml`).

**Classes shared by packages.** Several packages may declare a class with
the same code - for example a national and an international entity
description both declaring ``PERSON``. The class then exists once: the
same entities, rules and references use it. Each package states its own
name, in its own language, and its own ``read-only``. Declaring a code
declared by another package asserts that it is the same concept. The
packages must agree on the parent: a declaration with another parent
refuses the import. The class is removed with the last package declaring
it.

The names of all declarations are texts of the class in the languages of
the declaring packages, resolved like translations: the reader's language
first; when several packages give a text in that language, the package
deeper in the dependency order wins, ties by package code; otherwise the
name in the language of the installation. When two packages that do not
depend on each other give different names in one language, an installation
fixes the choice with a small package depending on both that translates
the name.

rul_part_type.xml
=================

Part types of entities the package declares: the sections of an entity
such as names, creation or relations.

.. code-block:: xml

   <part-types>
       <part-type code="PT_NAME">
           <name>Označení</name>
           <repeatable>true</repeatable>
       </part-type>
       <part-type code="PT_CRE">
           <name>Vznik</name>
           <child_part>PT_REL</child_part>
           <repeatable>false</repeatable>
       </part-type>
   </part-types>

``code`` (required, attribute), ``name`` (required)
   Code of the part type and its name in the language of the package.

``child_part``
   Part type of parts attached to a part of this type (relations of a
   creation); declared in the same file or by a package this one depends
   on.

``repeatable``
   An entity may have several parts of the type.

Part types are shared by packages like classes: several packages may
declare the same code, the part type exists once, and the names resolve
by language like the names of classes. ``child_part`` and ``repeatable``
are not checked across declarations; they come from the winning
declaration (the package deeper in the dependency order, ties by package
code). Declarations of one code should agree; an installation forces a
value with a small package depending on both. The part type is removed
with the last package declaring it; the import is refused while parts of
entities use it or rules of another package refer to it.

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
   digital archive), and for the entity rule set ``AP_MAPPING_TYPE``
   (items sent to the CAM system; needed only when entities of the rule
   set are exchanged with CAM). Items computed from an entity are an
   entity rule (``AUTO_ITEMS`` in :file:`rul_entity_rule.xml`).

rul_rule_set/<RS>/rul_arrangement_extension.xml and rul_extension_rule.xml
==========================================================================

Arrangement extensions are optional sets of rules that archivists switch on
for a unit of description and the units below it. They belong to rule sets
of type ``ARRANGEMENT``; an entity rule set declares its rules in
:file:`rul_entity_rule.xml` and refuses these files.

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
(attribute): the funds of the rule set are validated again, as for rule
sets.

rul_rule_set/<RS>/rul_entity_rule.xml
=====================================

Rules of an entity rule set (``rule-type`` ``ENTITY``): which items a part
of an entity offers, how the entity is validated, and how the names of its
parts are built. A rule runs only for entities whose scope uses the rule
set. The files are in :file:`rul_rule_set/<RS>/rules/`.

.. code-block:: xml

   <entity-rules>
       <entity-rule filename="available_items/GLOBAL.drl" kind="AVAILABLE_ITEMS" priority="100"/>
       <entity-rule filename="available_items/PT_NAME.drl" kind="AVAILABLE_ITEMS"
                    part-type="PT_NAME" priority="100"/>
       <entity-rule filename="available_items/PERSON/PT_NAME.drl" kind="AVAILABLE_ITEMS"
                    ap-type="PERSON" part-type="PT_NAME" priority="100"/>
       <entity-rule filename="validation/PERSON/GLOBAL.drl" kind="VALIDATION"
                    ap-type="PERSON" priority="100" compatibility-rul-package="31"/>
       <entity-rule filename="index/PT_BODY.groovy" kind="INDEX"
                    part-type="PT_BODY" priority="100"/>
       <entity-rule filename="index/PERSON/PT_NAME.groovy" kind="INDEX"
                    ap-type="PERSON" part-type="PT_NAME" priority="100"/>
   </entity-rules>

``filename``, ``kind`` and ``priority`` (required, attributes)
   ``kind`` is ``AVAILABLE_ITEMS`` (items available in a part, evaluated
   for one part), ``VALIDATION`` (validation of the whole entity) - both
   Drools files - ``INDEX``: a Groovy script building the name and
   indexes of a part (display name, sort name, the preferred name of the
   entity, key values) - or ``AUTO_ITEMS``: a Groovy script computing items
   of the whole entity (offered to the user and checked by validation; it
   gets the entity as ``AE`` and returns a list of ``GroovyItem``; without
   a rule the entity has no computed items; no ``part-type``). For
   ``INDEX`` and ``AUTO_ITEMS`` only the most specific rule applies:
   a rule of the class or its nearest parent before a rule of all classes,
   a rule of the part type before a rule of all parts, the highest
   priority. A part without a script cannot be saved.

The ``INDEX`` script receives the part as ``PART`` and returns a
``GroovyResult``:

- ``setDisplayName`` (required): the name of the part; for the preferred
  name part it is the name of the entity in lists, search and sorting,
  for the description part (``PT_BODY``) the description of the entity.
- ``setPtPreferName`` on the preferred name part (``PART.isPreferred()``):
  the name must be unique in the scope; a duplicate gets a suffix.
- ``setSortName``: the order of parts of one type.
- ``addIndex("SHORT_NAME", ...)`` on the preferred name: the short name
  of an institution described by the entity and of the institution in
  statistics (otherwise the display name is used).
- ``setKeyValue`` and further ``addIndex`` values as the rule set needs.

``ap-type`` (attribute)
   Code of the entity class. The rule applies to the class and its
   subclasses; without it, to all classes.

``part-type`` (attribute)
   Code of the part type; without it, the rule applies to all parts.
   Validation rules run for all parts regardless of it.

``compatibility-rul-package`` (attribute)
   A package version: when the package is upgraded from a lower version,
   the entities of the class (all, without ``ap-type``) in the scopes of the
   rule set are validated again (and their names built again).

``AVAILABLE_ITEMS`` and ``VALIDATION`` rules run in this order: rules for all classes, then for each class from
the root of the class hierarchy down to the class of the entity; on each
level the rules without a part type before those of the part; by priority
within a group. A package may contribute rules to the entity rule set of
another package in :file:`rul_rule_set/<FOREIGN>/rul_entity_rule.xml`. The
class and the part type must exist (in the package or its dependencies);
an unknown one refuses the import. A package cannot remove a class or part
type that entity rules of another package refer to.

rul_rule_set/<RS>/rul_ap_type.xml
=================================

Entity classes an entity rule set uses (its members). Entities of a scope
can only have classes the rule set of the scope offers, and only an
assignable class can be chosen for an entity, when it is created or its
class or scope is changed. Without this file the rule set offers all
classes, assignable unless ``read-only`` in :file:`ap_type.xml`.

.. code-block:: xml

   <ap-types>
       <ap-type code="PERSON" assignable="false"/>
       <ap-type code="PERSON_INDIVIDUAL"/>
   </ap-types>

``code`` (required, attribute)
   Code of a class of the package or of a package it depends on. A class
   may be used by several rule sets; its definition (name, parent) stays
   with the package that defines it.

``assignable`` (attribute)
   Whether entities may get the class in scopes of the rule set; without
   it, the opposite of ``read-only`` of the class. Abstract roots are
   members that are not assignable: they are shown in the class tree, and
   rules may refer to them.

A package may declare members of the entity rule set of another package in
:file:`rul_rule_set/<FOREIGN>/rul_ap_type.xml`. When several packages state
the same class for one rule set, the package deeper in the dependency order
wins. A class used by members of another package cannot be removed. The
rule set of a scope cannot be changed while the scope holds entities of a
class the new rule set does not offer, and the CAM import of an entity of
such a class into a scope fails.

The order of the file is the order of the class tree: a class takes its
position, a parent that is not listed takes the position of its first
listed subclass. The members of the owner of the rule set come first, then
those of contributing packages in dependency order (ties by package code),
each in the order of its file; a class listed by several packages keeps
its first position. Without the file, roots are ordered as declared and
subclasses by name.

rul_rule_set/<RS>/rul_part_type.xml
===================================

Part types an entity rule set offers, in the order in which the parts of an
entity are shown. Without this file the rule set offers all part types,
ordered by the ``parts-order`` UI setting of the rule set when present.

.. code-block:: xml

   <part-types>
       <part-type code="PT_NAME"/>
       <part-type code="PT_CRE"/>
       <part-type code="PT_EXT"/>
       <part-type code="PT_BODY"/>
   </part-types>

``code`` (required, attribute)
   Code of a part type of the package or of a package it depends on; a
   code may be listed once.

The entity detail does not offer a part type the rule set does not list
(the server does not refuse it); an entity that already has parts of it
still shows them, after the listed ones. Contributing packages add part types in
:file:`rul_rule_set/<FOREIGN>/rul_part_type.xml`, ordered as member
classes are. A part type listed by another package cannot be removed.

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
when the package is upgraded from a lower version, the objects of the
structured type are queued for regeneration.

Structure extensions (:file:`rul_structure_extension.xml`,
:file:`rul_structure_extension_definition.xml`) add optional definitions
to a structured type. Structured types serve archival description; the
names of parts of archival entities are built by ``INDEX`` entity rules
(:file:`rul_entity_rule.xml`), not by structured types named by part
codes as before.

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
   :file:`ap_external_id_type.xml`, :file:`par_institution_type.xml`, issue
   types and states, export and output filters, import transformations.
