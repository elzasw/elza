package cz.tacr.elza.service.output.ftsender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;

import com.lightcomp.ft.client.Transfer;
import com.lightcomp.ft.client.TransferStatus;
import com.lightcomp.ft.client.UploadRequest;
import com.lightcomp.ft.core.send.items.SourceItemReader;
import com.lightcomp.ft.xsd.v1.GenericDataType;

import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.service.DmsService;

public class OutputUploadRequest implements UploadRequest {
	
	private final GenericDataType genericDataType = new GenericDataType();
	
	private final String logId = UUID.randomUUID().toString();
	
	public enum Status {
		INIT,
		FAILED,
		CANCELLED,
		SUCCESS
	};
	
	private Status status = Status.INIT;

	private final List<ArrOutputFile> files = new ArrayList<>();
	
	private final DmsService dmsService;
	
	public OutputUploadRequest(final Integer fundNumber, 
							   final String findingAidCode,
							   final DmsService dmsService) {
		Validate.notNull(fundNumber);
		
		genericDataType.setType("POMUCKA");
		if(StringUtils.isEmpty(findingAidCode)) {
			genericDataType.setId(fundNumber.toString());
		} else {
			genericDataType.setId(String.join(",", fundNumber.toString(), findingAidCode));
		}
		this.dmsService = dmsService;
	}
	
	public Status getStatus() {
		return status;
	}

	@Override
	public GenericDataType getData() {
		return genericDataType;
	}

	@Override
	public String getLogId() {
		return logId;
	}

	@Override
	public void onTransferInitialized(Transfer transfer) {
		// nop

	}

	@Override
	public void onTransferProgress(TransferStatus status) {
		// nop

	}

	@Override
	public void onTransferCanceled() {
		status = Status.CANCELLED;
	}

	@Override
	public void onTransferFailed() {
		status = Status.FAILED;
	}

	@Override
	public void onTransferSuccess(GenericDataType response) {
		status = Status.SUCCESS;
	}

	@Override
	public SourceItemReader getRootItemsReader() {
		return new OutputFilesReader(files, dmsService);
	}

	public void addFiles(final List<ArrOutputFile> files) {
		this.files.addAll(files);		
	}

}
