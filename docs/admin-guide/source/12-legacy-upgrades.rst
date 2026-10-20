=========================
Appendix: Legacy Upgrades
=========================

Data fixes that may be needed when upgrading installations older than
2.9. Run them only when the described error occurs, always with the
application stopped and after a database backup.

Duplicate item objects (upgrade to 2.1.2 and later)
===================================================

The migration fails with::

   Migration failed for change set db/changelog/db.elza-1.5.xml::20210330155924::ppyt:
     Reason: liquibase.exception.DatabaseException: ERROR: could not create unique index "arr_item_object_pidx"
     Detail: Key (desc_item_object_id)=(45647) is duplicated.

Older versions could leave duplicate description elements. Assess the
extent of the inconsistency first. The duplicates are removed by these
three statements:

.. code-block:: sql

   DELETE FROM arr_cached_node WHERE node_id IN (
     SELECT DISTINCT node_id FROM arr_desc_item WHERE item_id IN (
       SELECT DISTINCT i1.item_id FROM arr_item i1
       JOIN arr_item i2 ON i1.desc_item_object_id = i2.desc_item_object_id
       WHERE i1.item_id < i2.item_id
         AND i1.delete_change_id IS NULL AND i2.delete_change_id IS NULL));

   DELETE FROM arr_desc_item WHERE item_id IN (
     SELECT DISTINCT i1.item_id FROM arr_item i1
     JOIN arr_item i2 ON i1.desc_item_object_id = i2.desc_item_object_id
     WHERE i1.item_id < i2.item_id
       AND i1.delete_change_id IS NULL AND i2.delete_change_id IS NULL);

   DELETE FROM arr_item WHERE item_id IN (
     SELECT DISTINCT i1.item_id FROM arr_item i1
     JOIN arr_item i2 ON i1.desc_item_object_id = i2.desc_item_object_id
     WHERE i1.item_id < i2.item_id
       AND i1.delete_change_id IS NULL AND i2.delete_change_id IS NULL);

Duplicate CAM bindings (upgrade to 2.3.4 and later)
===================================================

With an active connection to CAM (typically test or development
instances), the migration fails with::

   Migration failed for change set db/changelog/db.elza-1.5.xml::20211105113000::sergey.iryupin:
     Reason: liquibase.exception.DatabaseException: ERROR: could not create unique index "ap_binding_state_access_point_idx"
     Detail: Key (access_point_id, external_system_id)=(40906, 23) is duplicated.

Fix:

.. code-block:: sql

   CREATE TABLE tmp_fix_binding (binding_id integer, access_point_id integer);
   INSERT INTO tmp_fix_binding (binding_id, access_point_id)
   SELECT bs.binding_id, bs.access_point_id FROM ap_binding_state bs
   JOIN ap_binding b ON b.binding_id = bs.binding_id
   JOIN ap_binding_state bs2 ON bs.access_point_id = bs2.access_point_id
   JOIN ap_binding b2 ON b2.binding_id = bs2.binding_id
   WHERE bs.delete_change_id IS NULL AND bs2.delete_change_id IS NULL
     AND b.external_system_id = b2.external_system_id
     AND bs.binding_state_id <> bs2.binding_state_id;
   UPDATE arr_data_record_ref rr SET record_id = src.access_point_id, binding_id = NULL
   FROM tmp_fix_binding src WHERE src.binding_id = rr.binding_id;
   DELETE FROM ap_binding_item WHERE binding_id IN (SELECT binding_id FROM tmp_fix_binding);
   DELETE FROM ap_binding_state WHERE binding_id IN (SELECT binding_id FROM tmp_fix_binding);
   DELETE FROM ext_syncs_queue_item WHERE access_point_id IN (SELECT access_point_id FROM tmp_fix_binding);
   DELETE FROM ext_syncs_queue_item WHERE binding_id IN (SELECT binding_id FROM tmp_fix_binding);
   DELETE FROM ap_binding WHERE binding_id IN (SELECT binding_id FROM tmp_fix_binding);
   DROP TABLE tmp_fix_binding;

Language of the fund root (2.8.5 and later)
===========================================

Version 2.8.5 introduced the description element *Language of the archival
fund* (``ZP2015_MAJOR_LANG``). Before, the root of a fund usually carried
the item *Language* (``ZP2015_LANGUAGE``) in this meaning. The following
script moves it to the new item type:

.. code-block:: sql

   UPDATE arr_item i SET item_type_id = (SELECT item_type_id FROM rul_item_type WHERE code = 'ZP2015_MAJOR_LANG')
   WHERE i.item_id IN (
     SELECT i.item_id FROM arr_fund f
     JOIN arr_fund_version fv ON fv.fund_id = f.fund_id AND fv.lock_change_id IS NULL
     JOIN arr_node n ON n.node_id = fv.root_node_id
     JOIN arr_desc_item di ON di.node_id = n.node_id
     JOIN arr_item i ON i.item_id = di.item_id AND i.delete_change_id IS NULL
     JOIN rul_item_type t ON t.item_type_id = i.item_type_id
     WHERE code = 'ZP2015_LANGUAGE');

   DELETE FROM arr_cached_node WHERE node_id IN (
     SELECT fv.root_node_id FROM arr_fund f
     JOIN arr_fund_version fv ON fv.fund_id = f.fund_id AND fv.lock_change_id IS NULL
     JOIN arr_node n ON n.node_id = fv.root_node_id
     JOIN arr_desc_item di ON di.node_id = n.node_id
     JOIN arr_item i ON i.item_id = di.item_id AND i.delete_change_id IS NULL
     JOIN rul_item_type t ON t.item_type_id = i.item_type_id
     WHERE code = 'ZP2015_MAJOR_LANG');

Numerical scale (2.8.16 and later)
==================================

Version 2.8.16 allowed the description element *Scale* to be entered in a
numerical form (``ZP2015_SCALE_NUMERICAL``); before, only the text form
(``ZP2015_SCALE``) existed. The following script converts text values in
the form ``1:n`` to the numerical form; other values stay unchanged.

.. code-block:: sql

   -- copy matching text values to string data
   INSERT INTO arr_data_string
   SELECT dtt.data_id, regexp_replace(dtt.value, '\s+', '', 'g') FROM arr_item it
   JOIN arr_data_text dtt ON dtt.data_id = it.data_id
   JOIN rul_item_type rit ON rit.item_type_id = it.item_type_id AND rit.code = 'ZP2015_SCALE'
   JOIN arr_desc_item di ON di.item_id = it.item_id
   WHERE regexp_replace(dtt.value, '\s+', '', 'g') ~ '^\[?(1|10):[0-9]+\]?$'
     AND it.delete_change_id IS NULL;

   -- clear the cache of affected nodes
   DELETE FROM arr_cached_node WHERE node_id IN (
     SELECT node_id FROM arr_item it
     JOIN arr_desc_item dit ON dit.item_id = it.item_id
     JOIN arr_data_string ds ON ds.data_id = it.data_id
     JOIN arr_data_text dt ON dt.data_id = it.data_id
     JOIN rul_item_type rit ON rit.item_type_id = it.item_type_id AND rit.code = 'ZP2015_SCALE');

   -- change the data type
   UPDATE arr_data d SET data_type_id = (SELECT data_type_id FROM rul_item_type WHERE code = 'ZP2015_SCALE_NUMERICAL')
   WHERE data_id IN (
     SELECT it.data_id FROM arr_item it
     JOIN arr_data_string ds ON ds.data_id = it.data_id
     JOIN arr_data_text dt ON dt.data_id = it.data_id
     JOIN rul_item_type rit ON rit.item_type_id = it.item_type_id AND rit.code = 'ZP2015_SCALE');

   -- change the item type
   UPDATE arr_item it SET item_type_id = (SELECT item_type_id FROM rul_item_type WHERE code = 'ZP2015_SCALE_NUMERICAL')
   WHERE it.item_id IN (
     SELECT it.item_id FROM arr_item it
     JOIN arr_data_string ds ON ds.data_id = it.data_id
     JOIN arr_data_text dt ON dt.data_id = it.data_id
     JOIN rul_item_type rit ON rit.item_type_id = it.item_type_id AND rit.code = 'ZP2015_SCALE');

   -- remove the original text data
   DELETE FROM arr_data_text WHERE data_id IN (
     SELECT it.data_id FROM arr_item it
     JOIN arr_data_string ds ON ds.data_id = it.data_id
     JOIN arr_data_text dt ON dt.data_id = it.data_id
     JOIN rul_item_type rit ON rit.item_type_id = it.item_type_id AND rit.code = 'ZP2015_SCALE_NUMERICAL');
