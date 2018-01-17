package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;

public class DescriptionItemStructObjectRefImpl extends DescriptionItemStructObjectRef {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.STRUCTURED) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }

        throw new UnsupportedOperationException("Not yet implemented");
        /*
        StructuredObjectInfo sobjInfo = context.getSections().getCurrentSection().getStructuredObjectInfo(getSoid());
        if (sobjInfo == null) {
            throw new DEImportException("Referenced structured object not found, soId:" + getSoid());
        }
        ArrDataStructureRef data = new ArrDataStructureRef();
        data.setStructureData(packetInfo.getEntityReference(context.getSession()));
        
        String fulltext = ArrPacket.createFulltext(packetInfo.getStorageNumber(), packetInfo.getPacketType());
        return new ImportableItemData(data, fulltext);
        */
    }

}
