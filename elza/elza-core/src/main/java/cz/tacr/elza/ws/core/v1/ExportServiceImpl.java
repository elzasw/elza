package cz.tacr.elza.ws.core.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.writer.cam.CamExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlNameConsts;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.ws.core.v1.exportservice.ExportWorker;
import cz.tacr.elza.ws.types.v1.ExportRequest;
import cz.tacr.elza.ws.types.v1.ExportResponseData;
import cz.tacr.elza.ws.types.v1.IdentifierList;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "ExportService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.ExportService")
public class ExportServiceImpl implements ExportService {

    @Autowired
    @Qualifier("threadPoolTaskExecutorBA")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    ApplicationContext appCtx;

    public ExportServiceImpl() {

    }

    private DEExportParams createExportParams(ExportRequest request) {
        DEExportParams params = new DEExportParams();

        if (request.getEntities() != null && request.getEntities().getIdentifiers() != null) {
            addApids(params, request.getEntities().getIdentifiers());
        }
        return params;
    }

    private void addApids(DEExportParams params, IdentifierList identifiers) {
        List<String> request = identifiers.getIdentifier();
        if (request == null) {
            return;
        }
        // detect ID type
        List<Integer> apIds = new ArrayList<>(request.size());
        List<String> uuidList = new ArrayList<>();
        List<String> incorrect = new ArrayList<>();
        for (String ident : request) {
            if (ident.length() == 36) {
                uuidList.add(ident);
            } else {
                // convert to id
                try {
                    apIds.add(Integer.parseInt(ident));
                } catch (NumberFormatException nfe) {
                    incorrect.add(ident);
                }
            }
        }

        // TODO: map uuid to id
        /*
        if (CollectionUtils.isNotEmpty(uuidList))
        {
            getEntitiesById(convertor, uuidList);
            IdentifierList identsList = request.getIdentifiers();
            ObjectListIterator<String> oli = new ObjectListIterator<>(1000, request.getIdentifiers().getIdentifier());
        }
        */
        //List<Integer> rootNodeIds = params.getOutputNodeIds();

        params.setApIds(apIds);
    }

    @Override
    public ExportResponseData exportData(ExportRequest request) throws ExportRequestException {
        ExportResponseData erd = new ExportResponseData();

        DEExportParams params = createExportParams(request);
        // prepare converter
        String format = request.getRequiredFormat();
        if (XmlNameConsts.SCHEMA_URI.equals(format)) {
            // native export
        } else
        if (CamUtils.CAM_SCHEMA.equals(format)) {
            // fomat CAM
            params.setExportBuilder(new CamExportBuilder());
        } else {
            throw new ExportRequestException("Unrecognized schema: " + format);
        }
        
        // prepare sec context
        SecurityContext secCtx = SecurityContextHolder.getContext();
        Authentication auth = secCtx.getAuthentication();

        // Create piped output stream, wrap it in a final array so that the
        // OutputStream doesn't need to be finalized before sending to new Thread.
        PipedOutputStream out = new PipedOutputStream();
        try {
            InputStream in = new PipedInputStream(out);
            
            ExportWorker eew = appCtx.getBean(ExportWorker.class, out, params, auth);
            
            taskExecutor.execute(eew);

            DataSource ds = new ByteArrayDataSource(in, "application/octet-stream");
            DataHandler dataHandler = new DataHandler(ds);
            erd.setBinData(dataHandler);
        } catch (IOException e) {
            throw new SystemException(e);
        }

        return erd;
    }

}

