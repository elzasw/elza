package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.filters.ExportFilter;
import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;

public class StructObjectInfoDispatcher extends BaseLoadDispatcher<StructObjectInfoImpl> {

    private final SectionOutputStream os;

    private StructObjectInfo structObjectInfo;

    private ExportContext context;

    public StructObjectInfoDispatcher(ExportContext context, SectionOutputStream os) {
        this.context = context;
        this.os = os;
    }

    @Override
    public void onLoad(StructObjectInfoImpl result) {
        Validate.isTrue(structObjectInfo == null);
        this.structObjectInfo = result;
    }

    @Override
    protected void onCompleted() {
        ExportFilter exportFilter = context.getExportFilter();
        if (exportFilter != null) {
            StructObjectInfo filteredStructObj = exportFilter.processStructObj(structObjectInfo);
            if (filteredStructObj == null) {
                return;
            }
            os.addStructObject(filteredStructObj);
        } else {
            os.addStructObject(structObjectInfo);
        }
    }
}
