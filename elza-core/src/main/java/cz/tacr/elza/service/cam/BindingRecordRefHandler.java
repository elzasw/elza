package cz.tacr.elza.service.cam;

import cz.tacr.cam.schema.cam.EntityIdXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.service.cam.CamXmlFactory.EntityRefHandler;

public class BindingRecordRefHandler implements EntityRefHandler {

    private final ApBinding binding;

    public BindingRecordRefHandler(final ApBinding binding) {
        this.binding = binding;
    }

    @Override
    public EntityRecordRefXml createEntityRef(ArrDataRecordRef dataRecordRef) {
        // create record ref only for records with same binding
        if (dataRecordRef.getBinding() == null || !dataRecordRef.getBinding().getApExternalSystem()
                .getExternalSystemId().equals(binding.getApExternalSystem().getExternalSystemId())) {
            return null;
        }
        EntityRecordRefXml entityRecordRef = new EntityRecordRefXml();
        entityRecordRef.setEid(new EntityIdXml(Long.parseLong(dataRecordRef.getBinding().getValue())));
        return entityRecordRef;
    }

}
