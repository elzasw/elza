/**
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 20.06.2019.
 */
-- Insert new record (tacr) into MetadataSchemaRegistry
INSERT INTO MetadataSchemaRegistry VALUES (5,'http://tacr.cz/','tacr');

-- Insert new record (tacr.isElza) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'isElza', 'Is in the system Elza'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'isElza' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));
