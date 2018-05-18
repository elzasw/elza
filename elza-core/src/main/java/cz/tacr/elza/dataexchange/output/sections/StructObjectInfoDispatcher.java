package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;

public class StructObjectInfoDispatcher extends BaseLoadDispatcher<StructObjectInfo> {

    private final SectionOutputStream os;

    private StructObjectInfo structObjectInfo;

    public StructObjectInfoDispatcher(SectionOutputStream os) {
        this.os = os;
    }

    @Override
    public void onLoad(StructObjectInfo result) {
        Validate.isTrue(structObjectInfo == null);
        this.structObjectInfo = result;
    }

    @Override
    protected void onCompleted() {
        os.addStructObject(structObjectInfo);
    }
}
