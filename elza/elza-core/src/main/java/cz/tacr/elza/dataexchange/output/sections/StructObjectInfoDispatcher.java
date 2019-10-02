package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;

public class StructObjectInfoDispatcher extends BaseLoadDispatcher<StructObjectInfoImpl> {

    private final SectionOutputStream os;

    private StructObjectInfo structObjectInfo;

    public StructObjectInfoDispatcher(SectionOutputStream os) {
        this.os = os;
    }

    @Override
    public void onLoad(StructObjectInfoImpl result) {
        Validate.isTrue(structObjectInfo == null);
        this.structObjectInfo = result;
    }

    @Override
    protected void onCompleted() {
        os.addStructObject(structObjectInfo);
    }
}
