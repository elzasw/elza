/**
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 09.07.2019.
 */

-- Insert new record (tacr.duration) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'duration', 'Technical metadata'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'duration' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));

-- Insert new record (tacr.imageHeight) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'imageHeight', 'Technical metadata'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'imageHeight' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));

-- Insert new record (tacr.imageWidth) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'imageWidth', 'Technical metadata'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'imageWidth' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));

-- Insert new record (tacr.sourceXdimUnit) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'sourceXdimUnit', 'Technical metadata'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'sourceXdimUnit' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));

-- Insert new record (tacr.sourceXdimValue) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'sourceXdimValue', 'Technical metadata'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'sourceXdimValue' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));

-- Insert new record (tacr.sourceXdimUnit) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'sourceXdimUnit', 'Technical metadata'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'sourceXdimUnit' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));

-- Insert new record (tacr.sourceXdimValue) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'sourceXdimValue', 'Technical metadata'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'sourceXdimValue' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));

-- Insert new record (tacr.sourceXdimValue) into MetadataFieldRegistry
INSERT INTO metadatafieldregistry (metadata_schema_id, element, scope_note)
  SELECT (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'), 'elzaDidId', 'Technical metadata'
    WHERE NOT EXISTS (SELECT metadata_field_id,element,scope_note FROM metadatafieldregistry WHERE element = 'elzaDidId' AND qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id='tacr'));
