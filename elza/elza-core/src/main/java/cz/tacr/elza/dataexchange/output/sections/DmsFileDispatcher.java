package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;

public class DmsFileDispatcher
        extends BaseLoadDispatcher<DmsFileInfoImpl> {
    private final SectionOutputStream os;
    private DmsFileInfoImpl fileInfoImpl;

    public DmsFileDispatcher(SectionOutputStream outputStream) {
        this.os = outputStream;
    }

    @Override
    public void onLoad(DmsFileInfoImpl result) {
        Validate.isTrue(fileInfoImpl == null);
        this.fileInfoImpl = result;
    }

    @Override
    protected void onCompleted() {
        os.addFile(fileInfoImpl);
    }

}
