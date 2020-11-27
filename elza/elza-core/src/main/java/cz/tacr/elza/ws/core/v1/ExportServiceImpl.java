package cz.tacr.elza.ws.core.v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import cz.tacr.elza.service.AccessPointDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import cz.tacr.elza.common.io.FilterInputStreamWithException;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.db.HibernateConfiguration;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.cam.CamExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlNameConsts;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.ws.core.v1.exportservice.ExportWorker;
import cz.tacr.elza.ws.core.v1.exportservice.ExportWorker.ErrorHandler;
import cz.tacr.elza.ws.types.v1.EntityInfo;
import cz.tacr.elza.ws.types.v1.EntityUpdates;
import cz.tacr.elza.ws.types.v1.ErrorDescription;
import cz.tacr.elza.ws.types.v1.ExportRequest;
import cz.tacr.elza.ws.types.v1.ExportResponseData;
import cz.tacr.elza.ws.types.v1.IdentifierList;
import cz.tacr.elza.ws.types.v1.Items;
import cz.tacr.elza.ws.types.v1.SearchEntity;
import cz.tacr.elza.ws.types.v1.SearchEntityResult;
import cz.tacr.elza.ws.types.v1.SearchEntityUpdates;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "ExportService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.ExportService")
public class ExportServiceImpl implements ExportService {

    private static Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    @Autowired
    @Qualifier("threadPoolTaskExecutorBA")
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    ApplicationContext appCtx;

    @Autowired
    ApAccessPointRepository accessPointRepository;

    @Autowired
    AccessPointService accessPointService;
    
    @Autowired
    StaticDataService staticDataService;

    @Autowired
    GroovyService groovyService;

    @Autowired
    SchemaManager schemaManager;

    @Autowired
    ApChangeRepository changeRepository;

    @Autowired
    AccessPointDataService apDataService;

    public ExportServiceImpl() {

    }

    // TODO: run this method in transaction
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
                // convert to id to number
                try {
                    apIds.add(Integer.parseInt(ident));
                } catch (NumberFormatException nfe) {
                    incorrect.add(ident);
                }
            }
        }

        // map uuid to id
        if (CollectionUtils.isNotEmpty(uuidList))
        {
            List<List<String>> lists = Lists.partition(uuidList, HibernateConfiguration.MAX_IN_SIZE);
            for (List<String> subList : lists) {
                List<ApAccessPointInfo> infoList = accessPointRepository.findActiveInfoByUuids(subList);
                // check if al items were found
                if (infoList.size() != subList.size()) {
                    checkMissingIds(subList, infoList);
                }
                apIds.addAll(infoList.stream().map(ApAccessPointInfo::getAccessPointId).collect(Collectors.toList()));
            }
        }
        //List<Integer> rootNodeIds = params.getOutputNodeIds();

        params.setApIds(apIds);
    }

    private void checkMissingIds(List<String> subList, List<ApAccessPointInfo> infoList) throws ExportRequestException {
        
        Set<String> foundItems = infoList.stream().map(ApAccessPointInfo::getUuid).collect(Collectors.toSet());
        
        List<String> missing = subList.stream().filter(e -> !foundItems.contains(e)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(missing)) {
            log.info("Cannot find some of requested entities: {}", missing);
            cz.tacr.elza.ws.types.v1.ErrorDescription ed = WSHelper.prepareErrorDescription("Failed to find requested entities.",
                                                                                            "Missing items: " + String
                                                                                                    .join(",",
                                                                                                          missing));

            throw new ExportRequestException(ed.getUserMessage(), ed);
        }
    }

    @Override
    public ExportResponseData exportData(ExportRequest request) throws ExportRequestException {
        ExportResponseData erd = new ExportResponseData();

        DEExportParams params = createExportParams(request);
        // prepare converter
        String format = request.getRequiredFormat();
        
        ExportBuilder exportBuilder;

        if (XmlNameConsts.SCHEMA_URI.equals(format)) {
            // native export
            exportBuilder = new XmlExportBuilder();
        } else
        if (CamUtils.CAM_SCHEMA.equals(format)) {
            // fomat CAM
            exportBuilder = new CamExportBuilder(staticDataService, groovyService, schemaManager, apDataService);
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
            final FilterInputStreamWithException fis = new FilterInputStreamWithException(in);
            
            ErrorHandler asyncErrorHandler = (e) -> {
            	fis.setException(e);
            };            

            ExportWorker eew = appCtx.getBean(ExportWorker.class, out, exportBuilder,
                                              params, auth, asyncErrorHandler);
    
            taskExecutor.execute(eew);

            DataSource ds = new ByteArrayDataSource(fis, "application/octet-stream");
            DataHandler dataHandler = new DataHandler(ds);
            erd.setBinData(dataHandler);
        } catch (IOException e) {
            throw new SystemException(e);
        }

        return erd;
    }

    @Transactional
    @Override
    public SearchEntityResult searchEntity(SearchEntity request) throws SearchEntityException {
        String entityId = request.getEntityId();
        
        ApAccessPoint ap;
        if(StringUtils.isBlank(entityId)) {
            Items items = request.getItems();
            Validate.notNull(items);
            List<Object> itemList = items.getStrOrLongOrEnm();
            
            List<ApAccessPoint> aps = accessPointService.findAccessPointsBySinglePartValues(itemList);

            if (aps.size() > 1) {
                ErrorDescription ed = WSHelper.prepareErrorDescription("Too many results: " + aps.size(),
                                                                       String.valueOf(aps.size()));
                throw new SearchEntityException(ed.getUserMessage(), ed);
            }
            if (aps.size() == 1) {
                ap = aps.get(0);
            } else {
                ap = null;
            }
        } else {
            if(entityId.length()==36) {
                ap = accessPointService.getAccessPointByUuid(entityId);
            } else {
                Integer id = Integer.parseInt(entityId);
                ap = accessPointService.getAccessPoint(id);
            }
        }

        SearchEntityResult result = new SearchEntityResult();
        if (ap != null) {
            ApState apState = accessPointService.getStateInternal(ap);
            ApFactory apFactory = appCtx.getBean(ApFactory.class);
            ApAccessPointVO apVo = apFactory.createVO(apState, true);

            EntityInfo entityInfo = new EntityInfo();
            entityInfo.setEntityId(ap.getAccessPointId().toString());
            entityInfo.setUuid(ap.getUuid());
            entityInfo.setPrefName(apVo.getName());
            entityInfo.setBriefDesc(apVo.getDescription());
            entityInfo.setEntityClass(apState.getApType().getCode());

            result.setEntityInfo(entityInfo);
        }
        return result;
    }

    @Override
    public EntityUpdates searchEntityUpdates(SearchEntityUpdates request) throws CoreServiceException {
        int fromId;
        if (request == null || request.getFromTrans() == null) {
            fromId = 0;
        } else {
            fromId = Integer.valueOf(request.getFromTrans());
        }
        int toId = changeRepository.findTop1ByOrderByChangeIdDesc().getChangeId();

        List<String> uuids = accessPointRepository.findAccessPointUuidChangedOrDeleted(fromId);

        IdentifierList identifierList = new IdentifierList();
        identifierList.getIdentifier().addAll(uuids);

        EntityUpdates updates = new EntityUpdates();
        updates.setFromTrans(String.valueOf(fromId));
        updates.setToTrans(String.valueOf(toId));
        updates.setEntityIds(identifierList);

        return updates;
    }
}
