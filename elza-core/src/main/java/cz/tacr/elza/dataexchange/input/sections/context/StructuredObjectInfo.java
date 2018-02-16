package cz.tacr.elza.dataexchange.input.sections.context;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.domain.ArrStructuredObject;

public class StructuredObjectInfo extends EntityIdHolder<ArrStructuredObject> {
    /*
    private final RulStructureType structuredType;
    */
    private final String text;

    public StructuredObjectInfo(/*RulStructureType structuredType,*/ String text) {
        super(ArrStructuredObject.class);
        this.text = text;
    }
    /*
    public RulPacketType getPacketType() {
        return packetType;
    }

    public String getStorageNumber() {
        return storageNumber;
    }
    */
}
