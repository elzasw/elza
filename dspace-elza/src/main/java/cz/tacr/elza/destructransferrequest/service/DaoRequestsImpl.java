package cz.tacr.elza.destructransferrequest.service;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.DaoImportScheduler;
import cz.tacr.elza.metadataconstants.MetadataConstantService;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.elza.DestructTransferRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Implementace webových služeb pro komunikaci se systému Elza (server)
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 21.06.2019.
 */
@Component
@javax.jws.WebService(
        serviceName = "CoreService",
        portName = "DaoCoreRequests",
        targetNamespace = "http://dspace.tacr.cz/ws/core/v1",
        endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoRequests")

public class DaoRequestsImpl implements DaoRequests{
    private String SEPARATOR = ";";
    private String BUNDLE  = "ORIGINAL";

    @Autowired
    private DescructTransferRequestService descructTransferRequestService;

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();;
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static Logger log = Logger.getLogger(DaoImportScheduler.class);

    @Override
    public String postDestructionRequest(DestructionRequest destructionRequest) throws DaoServiceException {
        log.info("Spuštěna metoda postDestructionRequest.");
        String result = null;
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
        } catch (Exception e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }
        try {
            DestructTransferRequest destructRequest = descructTransferRequestService.findByIdentifier(context, destructionRequest.getIdentifier());
            if (destructRequest != null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + destructionRequest.getIdentifier() +
                        " již v systému existuje.");
            }

            DaoIdentifiers daoIdentifiers = destructionRequest.getDaoIdentifiers();
            if (daoIdentifiers == null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + destructionRequest.getIdentifier() +
                        " neobsahuje žádné identifikátory.");
            }
            log.info("Kontroluji identifikátory skartovaných digitalizátů.");
            List<String> identifierList = daoIdentifiers.getIdentifier();
            String identifierStr = null;
            for (String identifier : identifierList) {
                checkItemIdentifier(context, UUID.fromString(identifier));
                if (StringUtils.isNotEmpty(identifierStr)) {
                    identifierStr = identifierStr + SEPARATOR + identifier;
                } else {
                    identifierStr = identifier;
                }
            }

            log.info("Ukládám požadavek na skartaci digitalizátu.");
            destructRequest = new DestructTransferRequest();
            initDestructionRequest(destructionRequest, destructRequest);
            destructRequest.setDaoIdentifiers(identifierStr);
            descructTransferRequestService.create(context, destructRequest);
            context.complete();

            result = destructRequest.getUuid();
        }
        catch (Exception e) {
            context.abort();
            throw new DaoServiceException("Chyba při ukládání požadavku " + destructionRequest.getIdentifier() + " do databáze: " + e);
        }

        log.info("Ukončena metoda postDestructionRequest");
        return result;
    }

    @Override
    public String postTransferRequest(TransferRequest transferRequest) throws DaoServiceException {
        log.info("Spuštěna metoda postTransferRequest.");
        String result = null;
        Context context = new Context();
        try {
            context = ContextUtils.createContext();
            DestructTransferRequest destructRequest = descructTransferRequestService.findByIdentifier(context, transferRequest.getIdentifier());
            if (destructRequest != null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + transferRequest.getIdentifier() +
                        " již v systému existuje.");
            }

            DaoIdentifiers daoIdentifiers = transferRequest.getDaoIdentifiers();
            if (daoIdentifiers == null) {
                throw new ProcessingException("Požadavek DestructionRequest Identifier=" + transferRequest.getIdentifier() +
                        " neobsahuje žádné identifikátory.");
            }
            log.info("Kontroluji identifikátory delimitovaných digitalizátů.");
            List<String> identifierList = daoIdentifiers.getIdentifier();
            String identifierStr = null;
            for (String identifier : identifierList) {
                checkItemIdentifier(context, UUID.fromString(identifier));
                if (StringUtils.isNotEmpty(identifierStr)) {
                    identifierStr = identifierStr + SEPARATOR + identifier;
                } else {
                    identifierStr = identifier;
                }
            }

            log.info("Ukládám požadavek na delimitaci digitalizátu.");
            destructRequest = new DestructTransferRequest();
            initTransferRequest(transferRequest, destructRequest);
            destructRequest.setDaoIdentifiers(identifierStr);
            descructTransferRequestService.create(context, destructRequest);
            context.complete();

            result = destructRequest.getUuid();
        } catch (Exception e) {
            context.abort();
            throw new DaoServiceException("Chyba při ukládání požadavku " + transferRequest.getIdentifier() + " do databáze: " + e);
        }

        log.info("Ukončena metoda postTransferRequest");
        return result;
    }

    @Override
    public DaosSyncResponse syncDaos(DaosSyncRequest daosSyncRequest) throws DaoServiceException {
        log.info("Spuštěna metoda DaosSyncResponse.");
        DaosSyncResponse daosSyncResponse = new DaosSyncResponse();

        List<DaoSyncRequest> daoSyncRequestList = daosSyncRequest.getDaoSyncRequest();
        if (daoSyncRequestList.isEmpty()) {
            return daosSyncResponse;
        }

        Context context = new Context();
        try {
            context = ContextUtils.createContext();
        } catch (Exception e) {
            context.abort();
            throw new ProcessingException("Chyba při inicializaci contextu: " + e.getMessage());
        }

        Daoset daoset = new Daoset();
        List<Dao> daoList = daoset.getDao();
        NonexistingDaos nonexistingDaos = new NonexistingDaos();
        List<String> nonexistingDaoId = nonexistingDaos.getDaoId();
        for (DaoSyncRequest daoSyncRequest : daoSyncRequestList) {
            UUID uuId = UUID.fromString(daoSyncRequest.getDaoId());
            log.info("Vyhledávám položku digitalizátu Uuid=" + uuId + ".");
            Item item = getItem(context, uuId);
            if (item != null) {
                log.info("Aktualizuji metadata položky digitalizátu Uuid=" + uuId + ".");
                //TODO:cacha doplnit po upřesnění vstupních dat


                log.info("Zapisuji technická metadata položky digitalizátu Uuid=" + uuId + ".");
                List<Bundle> bundleList = item.getBundles();
                for (Bundle bundle : bundleList) {
                    if (bundle.getName().contains(BUNDLE)) {
                        Dao dao = new Dao();
                        FileGroup fileGroup = new FileGroup();
                        List<File> fileList = fileGroup.getFile();

                        List<Bitstream> bitstreamList = bundle.getBitstreams();
                        for (Bitstream bitstream : bitstreamList) {
                            File file = new File();

                            file.setIdentifier(bitstream.getID().toString());
                            BitstreamFormat format = null;
                            try {
                                format = bitstream.getFormat(context);
                            } catch (Exception e) {
                                context.abort();
                                throw new ProcessingException("Chyba při načtení formátu z bitstreamu: " + e.getMessage());
                            }
                            if (format != null) {
                                file.setMimetype(format.getMIMEType());
                            }

                            file.setChecksumType(convertStringToChecksumType(bitstream.getChecksumAlgorithm()));
                            file.setChecksum(bitstream.getChecksum());
                            file.setSize(bitstream.getSizeBytes());
//                            file.setCreated();  //TODO:cacha


                            String[] techMataData = MetadataConstantService.getTechMetaData();
                            for (String techMataDataCode : techMataData) {
                                String[] mt = MetadataConstantService.getMetaData(techMataDataCode);

                                List<MetadataValue> metadataValueList = bitstreamService.getMetadata(bitstream, mt[0], mt[1], mt[2], Item.ANY);
                                for (MetadataValue metadataValue : metadataValueList) {
                                    switch (techMataDataCode) {
                                        case "DURATION":
                                            file.setDuration(metadataValue.getValue());
                                            break;
                                        case "IMAGEHEIGHT":
                                            file.setImageHeight(convertStringToBigInteger(metadataValue.getValue()));
                                            break;
                                        case "IMAGEWIDTH":
                                            file.setImageWidth(convertStringToBigInteger(metadataValue.getValue()));
                                            break;
                                        case "SOURCEXDIMUNIT":
                                            file.setSourceXDimensionUnit(convertStringToUnitOfMeasure(metadataValue.getValue()));
                                            break;
                                        case "SOURCEXDIMVALUVALUE":
                                            file.setSourceXDimensionValue(Float.valueOf(metadataValue.getValue()));
                                            break;
                                        case "SOURCEYDIMUNIT":
                                            file.setSourceYDimensionUnit(convertStringToUnitOfMeasure(metadataValue.getValue()));
                                            break;
                                        case "SOURCEYDIMVALUVALUE":
                                            file.setSourceYDimensionValue(Float.valueOf(metadataValue.getValue()));
                                            break;
                                    }
                                }
                            }
                            if (file != null) {
                                fileList.add(file);
                            }
                        }
                        dao.setIdentifier(bundle.getID().toString());
                        dao.setFileGroup(fileGroup);
                        daoList.add(dao);
                    }
                }
            } else {
                log.info("Neexistující položka digitalizátu Uuid=" + uuId + ".");
                nonexistingDaoId.add(daoSyncRequest.getDaoId());
            }
        }

        daosSyncResponse.setDaoset(daoset);
        daosSyncResponse.setNonexistingDaos(nonexistingDaos);

        log.info("Ukončena metoda DaosSyncResponse");
        return daosSyncResponse;
    }


    private Item getItem(Context context, UUID uuId) {
        Item item = null;
        try {
            item = itemService.find(context, uuId);
        } catch (Exception e) {
            throw new ProcessingException("Chyba při vyhledání položky digitalizátu (" + uuId + "): " + e.getMessage());
        }
        return item;
    }

    private void checkItemIdentifier(Context context, UUID uuId) {
        Item item = null;
        try {
            item = itemService.find(context, uuId);
        } catch (Exception e) {
            throw new ProcessingException("Chyba při vyhledání položky digitalizátu (" + uuId + "): " + e.getMessage());
        }

        if (item == null) {
            throw new UnsupportedOperationException("Digitalizát s Uuid=" + uuId + " nebyl v tabulce Item nalezen.");
        }
    }

    private static ChecksumType convertStringToChecksumType (String checksumAlgorithm) {
        if (StringUtils.isBlank(checksumAlgorithm)) {
            return null;
        }
        switch (checksumAlgorithm) {
            case "MD5":
                return ChecksumType.MD_5;
            case "SHA1":
                return ChecksumType.SHA_1;
            case "SHA256":
                return ChecksumType.SHA_256;
            case "SHA384":
                return ChecksumType.SHA_384;
            case "SHA512":
                return ChecksumType.SHA_512;
        }
        throw new ProcessingException("Pole ChecksumType " + checksumAlgorithm + " není podporováno.");
    }

    private static BigInteger convertStringToBigInteger (String bigNumber) {
        if (StringUtils.isBlank(bigNumber)) {
            return null;
        }
        return BigInteger.valueOf(Long.parseLong(bigNumber));
    }

    private static UnitOfMeasure convertStringToUnitOfMeasure (String uomCode) {
    switch (StringUtils.upperCase(uomCode)) {
        case "IN":
            return UnitOfMeasure.IN;
        case "MM":
            return UnitOfMeasure.MM;
    }
    throw new ProcessingException("Kód měrné jednotky " + uomCode + " není podporován.");
    }

    private MetadataValue getBitstreamMetadata(List<MetadataValue> metadataList, String metaDataCode) {
        final String[] mt = MetadataConstantService.getMetaData(metaDataCode);

        for (MetadataValue mdValue : metadataList) {
            MetadataField metadataField = mdValue.getMetadataField();
            if (metadataField != null) {
                MetadataSchema metadataSchema = metadataField.getMetadataSchema();
                String mdSchema = null;
                if (metadataSchema != null) {
                    mdSchema = metadataSchema.getName();
                }

                if (StringUtils.equals(mdSchema, mt[0]) &&
                    StringUtils.equals(metadataField.getElement(), mt[1]) &&
                    StringUtils.equals(metadataField.getElement(), mt[2])) {

                return mdValue;
                }
            }
        }
        return null;
    }

    private void initDestructionRequest(DestructionRequest destructRequest, DestructTransferRequest destTransfRequest) {
        destTransfRequest.setUuid(UUID.randomUUID().toString());
        destTransfRequest.setRequestType(DestructTransferRequest.RequestType.DESTRUCTION);
        destTransfRequest.setIdentifier(destructRequest.getIdentifier());
        destTransfRequest.setSystemIdentifier(destructRequest.getSystemIdentifier());
        destTransfRequest.setDescription(destructRequest.getDescription());
        destTransfRequest.setUserName(destructRequest.getUsername());
        destTransfRequest.setStatus(DestructTransferRequest.Status.QUEUED);
        destTransfRequest.setRequestDate(new Date());
    }

    private void initTransferRequest(TransferRequest transferRequest,DestructTransferRequest destTransfRequest) {
        destTransfRequest.setUuid(UUID.randomUUID().toString());
        destTransfRequest.setRequestType(DestructTransferRequest.RequestType.TRANSFER);
        destTransfRequest.setIdentifier(transferRequest.getIdentifier());
        destTransfRequest.setSystemIdentifier(transferRequest.getSystemIdentifier());
        destTransfRequest.setDescription(transferRequest.getDescription());
        destTransfRequest.setUserName(transferRequest.getUsername());
        destTransfRequest.setTargetFund(transferRequest.getTargetFund());
        destTransfRequest.setStatus(DestructTransferRequest.Status.QUEUED);
        destTransfRequest.setRequestDate(new Date());
    }

}
