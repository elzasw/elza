package cz.tacr.elza.service.cam;

import java.util.UUID;

import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.service.cam.CamXmlFactory.EntityRefHandler;

public class ApUuidRecordRefHandler implements EntityRefHandler {

    @Override
    public EntityRecordRefXml createEntityRef(ArrDataRecordRef dataRecordRef) {
        String uuid;
        if (dataRecordRef.getBinding() == null) {
            uuid = dataRecordRef.getRecord().getUuid();
        } else {
            String bindingValue = dataRecordRef.getBinding().getValue();
            try {
                // check if binding value is uuid
                UUID.fromString(bindingValue);
                uuid = bindingValue;
            } catch (IllegalArgumentException e) {
                // binding value is not UUID
                // reference cannot be propageted
                return null;
            }
        }

        EntityRecordRefXml entityRecordRef = new EntityRecordRefXml();

        UuidXml uuidXml = CamXmlFactory.getObjectFactory().createUuidXml();
        uuidXml.setValue(uuid);
        entityRecordRef.setEuid(uuidXml);
        return entityRecordRef;
    }

}
