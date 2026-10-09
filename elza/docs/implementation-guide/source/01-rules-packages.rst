==============
Rules Packages
==============

A rules package is a ZIP archive that defines what can be described in ELZA
and how: item types and their specifications, rule sets with their Drools
rules, structured types, output types and templates, bulk actions and
entity types. ELZA ships two packages:

- ``CZ_BASE`` - entity types, entity parts and their item types (the CAM
  rule set for archival entities), shared code lists such as languages;
- ``ZP2015`` - the Czech archival description rules
  (*Základní pravidla pro zpracování archiválií*), rule set ``ZP2015``.

Packages are imported in *Administration* > *Package management*, or
automatically at startup from the :file:`dpkg/` subdirectory of the working
directory: the imported packages, and those the configuration key
``elza.packages.enabled`` lists, with their dependencies (see the
administration guide).

Structure of a package
======================

The archive contains the package directory itself, without a parent
directory. Files at the root describe the package and its global
definitions; each rule set has a directory under :file:`rul_rule_set/`.

.. code-block:: text

   package.xml                     code, name, version, dependencies
   rul_item_type.xml               item types
   rul_item_spec.xml               specifications and their item types
   rul_rule_set.xml                rule sets owned by the package
   ap_type.xml                     entity classes
   rul_part_type.xml               part types of entities
   rul_structure_type.xml          structured types (and their definitions,
   rul_structure_definition.xml    extensions and scripts)
   ui_setting.xml                  package-level UI settings
   rul_rule_set/<RULE_SET>/
       rul_arrangement_rule.xml    rules of the rule set
       rul_arrangement_extension.xml   optional extensions of the rule set
       rul_extension_rule.xml      rules of those extensions
       rul_entity_rule.xml         rules and scripts of an entity rule set
       rul_ap_type.xml             entity classes of an entity rule set
       rul_part_type.xml           part types of an entity rule set, in order
       rul_output_type.xml, rul_template.xml, rul_policy_type.xml,
       rul_package_actions.xml, ui_setting.xml, ...
       rules/                      Drools (.drl) and Groovy rule files
       bulk_actions/               bulk action definitions (.yaml)
       templates/                  output templates

Files that a package does not need may be left out. The elements of each
file are described in :doc:`02-package-files`.

package.xml
===========

.. code-block:: xml

   <package>
     <code>ZP2015</code>
     <name>Základní pravidla pro zpracování archiválií</name>
     <version>344</version>
     <description>...</description>
     <dependencies>
       <dependency code="CZ_BASE" min-version="88"/>
     </dependencies>
   </package>

``code``
   Unique code of the package. It is also used as the prefix of the codes
   the package defines (see `Codes`_).

``version``
   Integer. Importing a package whose version is already installed is
   refused, so every change of the package content needs a higher version,
   otherwise it never reaches an installation that already has the package.

``dependencies``
   Packages this package builds on, with the minimal installed version.
   The import is refused when a dependency is missing or older. Packages
   are imported in dependency order at startup, and a package cannot be
   deleted while another installed package depends on it.

Codes
=====

The codes of item types and specifications are unique across all installed
packages, and a package cannot redefine an item type of another package.
Codes are referenced by rule files, templates, bulk actions and exported
data, so they are permanent: once data exist, a code cannot be renamed by a
new package version.

Every package therefore uses its own code prefix (``ZP2015_``, ``SRD_``,
``ADT_``), including packages that customize another package (see
:doc:`03-customization`).

Rule sets and rules
===================

A fund is described under exactly one rule set. The rule set decides which
item types and specifications are offered on each unit of description,
which of them are required, how new units of description are created and
how the description is validated. These decisions are Drools rules,
registered in :file:`rul_arrangement_rule.xml` of the rule set directory:

.. code-block:: xml

   <arrangement-rules>
       <arrangement-rule filename="AvailableItems.drl">
           <rule-type>ATTRIBUTE_TYPES</rule-type>
           <priority>100</priority>
       </arrangement-rule>
   </arrangement-rules>

The rule types used for archival description:

``ATTRIBUTE_TYPES``
   Which item types and specifications are possible, recommended, required
   or impossible on a unit of description, and whether they are repeatable.

``CONFORMITY_INFO``
   Validation of a unit of description; reports missing items and errors.

``CONFORMITY_IMPACT``
   Which other units of description must be validated again after a change.

``NEW_LEVEL``
   The scenarios offered when a new unit of description is added.

``ITEM_TYPE_FILTER``
   Additional item types belonging to the rule set (see
   :ref:`customization-item-types`).

The rule set's own item type filter is named in :file:`rul_rule_set.xml`
(``<rule-item-type-filter>``). It decides which item types the rule set
uses at all: the grid columns, the search filters, the add-item dialog and
the dictionary offered to the AI assistant are built from it.

Rules of one type run in the order of their priority, each file in a
session of its own, so a later file sees and may change the decisions of
the earlier ones. The ZP2015 and CZ_BASE rules use priority 100.

At the start of the ``ATTRIBUTE_TYPES`` evaluation every item type and every
specification is impossible; a rule has to allow it.

The specifications a rule can allow are those the rule set sees: the
specifications assigned to the item type by packages related to the package
of the rule set - the package itself, the packages it depends on and the
packages depending on it (addons), both transitively. A specification that
only an unrelated package assigns to a shared item type is not offered under
the rule set, even by a rule allowing every specification of the item type
(``ItemSpec() from $it.specs``). The same holds for the entity rules of an
entity rule set. See "Specifications shared by packages" in the chapter on
package files.

Order of specifications
=======================

The specifications of an item type are offered in this order:

1. by package, in dependency order: the specifications of a package come
   after those of the packages it depends on;
2. within a package, in the order of the ``<item-spec>`` elements in
   :file:`rul_item_spec.xml`;
3. a specification with ``view-after`` on its ``<item-type-assign>`` is
   placed directly after the named specification of the same item type
   (see :ref:`customization-spec-order`).

The order is recomputed for all item types whenever any package is
imported.
