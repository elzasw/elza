package cz.tacr.elza.service.da.vo;

import com.lightcomp.ft.simple.UploadRequestImpl;
import com.lightcomp.ft.xsd.v1.GenericDataType;

import java.nio.file.Path;

public class DaUploadRequestImpl extends UploadRequestImpl {

    private GenericDataType response;

    public DaUploadRequestImpl(Path dataDir, GenericDataType data) {
        super(dataDir, data);
    }

    @Override
    public void onTransferSuccess(GenericDataType response) {
        super.onTransferSuccess(response);
        this.response = response;
    }

    public GenericDataType getResponse() {
        return response;
    }
}
