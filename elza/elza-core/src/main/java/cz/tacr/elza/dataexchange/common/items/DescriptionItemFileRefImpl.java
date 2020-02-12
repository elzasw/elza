package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.sections.context.SectionContext;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.schema.v2.DescriptionItemFileRef;

public class DescriptionItemFileRefImpl
        extends DescriptionItemFileRef {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.FILE_REF) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }

        SectionContext section = context.getSections().getCurrentSection();
        ArrFile file = section.getFile(getFid());

        if (file == null) {
            throw new DEImportException("Referenced file not found, fid:" + getFid());
        }
        ArrDataFileRef data = new ArrDataFileRef();
        data.setFile(file);
        data.setDataType(dataType.getEntity());

        return new ImportableItemData(data, null);
    }

}
