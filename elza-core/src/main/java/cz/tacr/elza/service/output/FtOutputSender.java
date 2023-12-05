package cz.tacr.elza.service.output;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.ClientConfig;

import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.output.ftsender.OutputUploadRequest;
import cz.tacr.elza.service.output.ftsender.OutputUploadRequest.Status;

@Service(value = "FtOutputSender")
public class FtOutputSender implements OutputSender {

	private static final Logger logger = LoggerFactory.getLogger(FtOutputSender.class);

    @Value("${elza.findingAid.upload.url:null}")
    private String url;

    @Value("${elza.findingAid.upload.username:null}")
    private String username;

    @Value("${elza.findingAid.upload.password:null}")
    private String password;

    @Value("${elza.findingAid.upload.soapLogging:false}")
    private Boolean soapLogging;

    @Autowired
    private DmsService dmsService;

	private Client ftClient;

    @PostConstruct
    public void initializeClient() {
//    	if(url==null) { //TODO cxf 4 - file transfer compatibility
//    		return;
//    	}
//
//    	logger.info("Initializing FileTranferOutputSender, url: {}", url);
//
//    	ClientConfig clientConfig = new ClientConfig(url);
//    	clientConfig.setRecoveryDelay(10);
//    	if(username!=null) {
//            ClientConfig.Authorization authorization = new ClientConfig.Authorization();
//            authorization.setAuthorizationType("Basic");
//            authorization.setPassword(password);
//            authorization.setUsername(username);
//            clientConfig.setAuthorization(authorization);
//    	}
//    	if(soapLogging!=null) {
//    		clientConfig.setSoapLogging(soapLogging);
//    	}
//		// dispatch
//    	ftClient = FileTransfer.createClient(clientConfig);
//    	ftClient.start();
    }

    @PreDestroy
    public void destroyClient() {
//    	if(ftClient!=null) { //TODO cxf 4- file transfer compatibility
//    		logger.info("Stoping FileTranferOutputSender ...");
//
//    		ftClient.stop();
//
//    		logger.info("FileTranferOutputSender stopped.");
//
//    		ftClient = null;
//    	}
    }

    @Override
	public void send(ArrOutput output) {
//    	if(ftClient==null) { //TODO cxf 4 - file transfer compatibility
//    		logger.error("FileTranferOutputSender not initialized");
//			throw new SystemException("FileTranferOutputSender not initialized");
//    	}
//		// prepare data
//    	List<ArrOutputFile> files = dmsService.findOutputFiles(output.getFundId(), output);
//
//    	// prepare request
//    	OutputUploadRequest request = new OutputUploadRequest(output.getFund().getFundNumber(),
//    			output.getInternalCode(), dmsService);
//    	request.addFiles(files);
//
//		logger.info("Preparing upload request {} for output: {} - {}", request.getLogId(), output.getOutputId(), output.getName());
//
//		ftClient.uploadSync(request);
//
//		if(request.getStatus()!=Status.SUCCESS) {
//			logger.info("Upload request failed, status: {}", request.getStatus());
//			throw new SystemException("Upload request failed")
//				.set("requestId", request.getLogId())
//				.set("status", request.getStatus());
//		}
	}

}
