===========================
Customizing a Rule Set
===========================

An installation often needs more than the shipped rules offer: its own
identifier types, a few institution-specific item types, a recommendation
that applies only to its agendas. Such changes are made in an **addon
package**: a separate package that depends on the package it customizes and
contributes to that package's rule set. The customized package itself is
never modified, so it can be upgraded independently and the addon keeps
working.

An addon package can:

- add specifications to item types of another package and place them among
  the existing ones;
- define its own item types and make them part of another package's rule
  set;
- add rules to another package's rule set: availability of items,
  validation, scenarios of new units of description;
- restrict its rules to selected parts of a fund through an arrangement
  extension.

The examples below follow the test package
:file:`elza-core/src/test/resources/rules-addon-test` of the ELZA sources,
which customizes ZP2015.

Structure of an addon package
=============================

.. code-block:: text

   package.xml
   rul_item_type.xml                     own item types (optional)
   rul_item_spec.xml                     own specifications (optional)
   rul_rule_set/ZP2015/                  contributions to the ZP2015 rule set
       rul_arrangement_rule.xml
       rul_arrangement_extension.xml     (optional, for scoped rules)
       rul_extension_rule.xml            (optional, for scoped rules)
       rules/*.drl

The addon declares a dependency on the customized package:

.. code-block:: xml

   <package>
     <code>ADDON_TEST</code>
     <name>Addon test package</name>
     <version>2</version>
     <description>...</description>
     <dependencies>
       <dependency code="ZP2015" min-version="344"/>
     </dependencies>
   </package>

The addon does not list the ZP2015 rule set in a :file:`rul_rule_set.xml`
of its own; a directory :file:`rul_rule_set/ZP2015/` is enough. Its rules are
stored as part of the addon: deleting the addon removes them and restores
the rule set as the customized package defines it.

Conventions:

- **Code prefix.** All codes of the addon (item types, specifications, rule
  files, extensions) start with the addon's own prefix, never with the prefix
  of the customized package. A specification named ``ZP2015_...`` in an addon
  would collide with a future version of ZP2015.
- **Priority 200 or more.** The ZP2015 and CZ_BASE rules have priority 100.
  Rules of an addon must have a higher priority so they run after them and
  can rely on, and override, their decisions.
- **Dependency version.** ``min-version`` is the version of the customized
  package the addon was tested with.

Specifications for an item type of another package
==================================================

A specification is attached to an item type by ``<item-type-assign>``. The
item type may belong to another package:

.. code-block:: xml

   <item-specs>
       <item-spec code="ADT_OTHERID_TEST">
           <name>Addon identifier</name>
           <description>...</description>
           <shortcut>addon id</shortcut>
           <item-type-assign code="ZP2015_OTHER_ID" view-after="ZP2015_OTHERID_SIG"/>
       </item-spec>
   </item-specs>

.. _customization-spec-order:

Position of the specification
-----------------------------

Without ``view-after`` the specifications of an addon come after all
specifications of the packages it depends on. ``view-after`` names a
specification of the same item type after which the new one is placed, so
archivists find it next to related specifications:

- several specifications placed after the same specification keep the order
  in which the addon lists them;
- a specification may be placed after another specification of the addon,
  which forms a chain;
- the position is kept when the customized package is upgraded, because the
  order is recomputed after every import;
- when the named specification does not exist (or the placements form a
  cycle), the specification is placed last and the import logs a warning.

Offering the specification
--------------------------

Attaching a specification does not yet offer it to archivists: during the
evaluation of a unit of description every specification starts as
impossible, and only a rule allows it. Some ZP2015 item types allow all
their specifications generically (``ZP2015_OTHER_ID``,
``ZP2015_UNIT_DAMAGE_TYPE`` and the languages); a specification added to
them is offered wherever the item type is. For other item types the addon
adds a rule:

.. code-block:: text

   rule "ADT_002 Addon identifier specification is possible on folders"
   no-loop
   when
       $activeLevel : ActiveLevel( )
       DescItem(type=="ZP2015_LEVEL_TYPE" && specCode=="ZP2015_LEVEL_FOLDER") from $activeLevel.descItems
       $itemType : RulItemTypeExt(code == "ZP2015_OTHER_ID")
       $itemSpec : RulItemSpecExt(code == "ADT_OTHERID_TEST") from $itemType.rulItemSpecList
   then
       $itemSpec.setType(RulItemSpec.Type.POSSIBLE);
       $itemSpec.setPolicyTypeCode("ZP2015_POL_BASIC");
   end

Do not add specifications to item types whose rules and validations work
with individual specifications by code (for example
``ZP2015_LEVEL_TYPE``): the customized rules would not know the new
specification.

Rules
=====

Rule files are registered in :file:`rul_rule_set/ZP2015/rul_arrangement_rule.xml`
of the addon:

.. code-block:: xml

   <arrangement-rules>
       <arrangement-rule filename="ADT_AvailableItems.drl">
           <rule-type>ATTRIBUTE_TYPES</rule-type>
           <priority>200</priority>
       </arrangement-rule>
       <arrangement-rule filename="ADT_Validation.drl">
           <rule-type>CONFORMITY_INFO</rule-type>
           <priority>200</priority>
       </arrangement-rule>
       <arrangement-rule filename="ADT_ItemTypeFilter.drl">
           <rule-type>ITEM_TYPE_FILTER</rule-type>
           <priority>200</priority>
       </arrangement-rule>
   </arrangement-rules>

Availability (``ATTRIBUTE_TYPES``)
   Facts: ``RulItemTypeExt`` for every item type with its
   ``RulItemSpecExt`` specifications, ``ActiveLevel`` (the evaluated unit of
   description) and ``Level`` for it and its ancestors, with their
   ``DescItem`` items. A rule calls ``setType``, ``setMinType`` (raise only),
   ``setRepeatable`` or ``setPolicyTypeCode``. Because the addon runs after
   the customized rules, it can also lower their decisions, for example
   ``setType(RulItemType.Type.IMPOSSIBLE)``.

Validation (``CONFORMITY_INFO``)
   Uses the global ``DataValidationResults dvResults``; for example
   ``dvResults.createMissing("ADT_STAGE", "Addon stage is missing (rule ADT_001).", "ZP2015_POL_BASIC")``.
   The message is shown to archivists as written, so write it in the
   language of the installation.

New units of description (``NEW_LEVEL``)
   Uses the global ``NewLevelApproaches results``; ``results.create(name)``
   adds a scenario, ``addDescItem(typeCode, specCode)`` presets its items.

Validation results already stored for existing units of description are not
recalculated when an addon is imported; validate the affected funds again.

.. note::

   Rule files are compiled when they are first used, not during the import.
   A syntax error in an addon rule appears as an error on the first unit of
   description evaluated after the import. Test an addon on a test
   installation first.

.. _customization-item-types:

Own item types
==============

An addon defines its own item types in :file:`rul_item_type.xml`, with its
own code prefix. Two things make an item type usable in the customized rule
set:

1. an ``ATTRIBUTE_TYPES`` rule of the addon allows it on the units of
   description where it belongs;
2. an ``ITEM_TYPE_FILTER`` rule adds it to the rule set's list of item types,
   which the grid, the search filters, the add-item dialog and the AI
   assistant use. The rule runs after the rule set's own filter, over the
   same facts (``cz.tacr.elza.drools.model.ItemType``):

.. code-block:: text

   package AddonTest;

   import cz.tacr.elza.drools.model.ItemType;

   rule "ADT filter"
   when
       $it: ItemType(code in ("ADT_STAGE"))
   then
       $it.setPossible();
   end

.. todo::

   Placing an own item type among the item types of the customized package
   (form groups, tree title, grid columns) - requires composition of UI
   settings from several packages, not available yet: a rule-set
   :file:`ui_setting.xml` in an addon is refused when the customized package
   already defines the same setting.

Rules for a part of a fund
==========================

A rule that should apply only to some funds, or to a part of a fund, is
attached to an **arrangement extension** of the addon. Archivists switch
the extension on for a unit of description in *Validation rules* on the
toolbar of the unit of description; it then applies to that unit and all
units below it.

:file:`rul_arrangement_extension.xml`:

.. code-block:: xml

   <arrangement-extensions>
       <arrangement-extension code="ADT_METRO">
           <name>Metro agenda</name>
       </arrangement-extension>
   </arrangement-extensions>

:file:`rul_extension_rule.xml`:

.. code-block:: xml

   <extension-rules>
       <extension-rule filename="ADT_Metro.drl" arrangement-extension="ADT_METRO">
           <rule-type>ATTRIBUTE_TYPES</rule-type>
           <priority>200</priority>
       </extension-rule>
   </extension-rules>

Extension rules use the same facts as the rules of the rule set and run
after them. The supported rule types are ``ATTRIBUTE_TYPES``,
``CONFORMITY_INFO``, ``CONFORMITY_IMPACT`` and ``NEW_LEVEL``.

A specification that should be offered only where the extension is active
needs care when the customized package allows all specifications of its
item type (``ZP2015_OTHER_ID``): an ``ATTRIBUTE_TYPES`` rule of the addon
lowers the specification to impossible everywhere, and the extension rule
allows it again where the extension is active.

.. todo::

   Not yet possible in an addon: placing own item types among the item types
   of the customized package; contributing UI settings for the customized
   rule set; extending a structured type of another package; removing a
   scenario of a new unit of description; renaming codes. See the
   development plan :file:`docs/rules-customization-layer-plan.md`.
