package cz.tacr.elza.ws;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.feature.FastInfosetFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.service.DaoImportService;
import cz.tacr.elza.metadataconstants.MetadataEnum;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.core.v1.DaoService;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.ChecksumType;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoBatchInfo;
import cz.tacr.elza.ws.types.v1.DaoPackage;
import cz.tacr.elza.ws.types.v1.Daoset;
import cz.tacr.elza.ws.types.v1.Datesingle;
import cz.tacr.elza.ws.types.v1.Did;
import cz.tacr.elza.ws.types.v1.File;
import cz.tacr.elza.ws.types.v1.FileGroup;
import cz.tacr.elza.ws.types.v1.RequestRevoked;
import cz.tacr.elza.ws.types.v1.UnitOfMeasure;
import cz.tacr.elza.ws.types.v1.Unitdatestructured;
import cz.tacr.elza.ws.types.v1.Unitid;
import cz.tacr.elza.ws.types.v1.Unittitle;

/**
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 03.07.2019.
 */
public class WsClient {

    private static final Logger logger = LoggerFactory.getLogger(WsClient.class);
    private static ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    /**
     * Vytvoří JaxWs klienta webové služby a vrátí proxy rozhraní.
     *
     * @param wsInterface třída rozhraní webové služby
     * @param url         adresa webové služby (bez wsdl)
     * @param loginName   Nepovinné uživatelské jméno pro přihlášení k webové službě
     * @param password    Nepovinné heslo pro přihlášení k webové službě
     * @return proxy rozhraní webové služby
     */
    private static <T> T getJaxWsRemoteInterface(Class<T> wsInterface, String url, @Nullable String loginName, @Nullable String password) {
        Assert.notNull(wsInterface, "Nebyla zadána třída webové služby.");
        Assert.hasText(url, "Nebylo zadáno url pro volání webové služby.");

        try {
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(wsInterface);
            factory.setAddress(url);
            factory.getFeatures().add(new FastInfosetFeature());
            //factory.setBindingId(SOAPBinding.SOAP12HTTP_MTOM_BINDING);

            HashMap<String, Object> properties = new HashMap<>();
            properties.put("jaxb-validation-event-handler", new DefaultValidationEventHandler());
            factory.setProperties(properties);

            //prihlaseni ke vzdalene sluzbe
            if (StringUtils.isNotBlank(loginName)) {
                factory.setUsername(loginName);
            }
            if (StringUtils.isNotBlank(password)) {
                factory.setPassword(password);
            }

            Object service = factory.create();


            return (T) service;

        } catch (Exception e) {
            throw new ProcessingException(e.getMessage(), e);
        }
    }

    public static void destructionRequestRevoked(final RequestRevoked requestRevoked) {
        final DaoRequestsService remoteInterface = getDaoRequests();
        try {
            remoteInterface.destructionRequestRevoked(requestRevoked);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new ProcessingException(e);
        }
    }

    public static void destructionRequestFinished(final String requestIdentifier) {
        final DaoRequestsService remoteInterface = getDaoRequests();
        try {
            remoteInterface.destructionRequestFinished(requestIdentifier);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new ProcessingException(e);
        }
    }

    public static void transferRequestRevoked(final RequestRevoked requestRevoked) {
        final DaoRequestsService remoteInterface = getDaoRequests();
        try {
            remoteInterface.transferRequestRevoked(requestRevoked);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new ProcessingException(e);
        }
    }

    public static void transferRequestFinished(final String requestIdentifier) {
        final DaoRequestsService remoteInterface = getDaoRequests();
        try {
            remoteInterface.transferRequestFinished(requestIdentifier);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new ProcessingException(e);
        }
    }

    public static void sendItemToElza(Item item, Context context) {

        DaoPackage daoPackage = new DaoPackage();
        daoPackage.setIdentifier(item.getID().toString());

        Collection collection = item.getOwningCollection();
        String fundId = collectionService.getMetadataFirstValue(collection, MetadataSchema.DC_SCHEMA, "description", "abstract", Item.ANY);
        daoPackage.setFundIdentifier(fundId);
        daoPackage.setRepositoryIdentifier(configurationService.getProperty("elza.repositoryCode"));

        DaoBatchInfo daoBatchInfo = new DaoBatchInfo();
        daoBatchInfo.setIdentifier(item.getName());
        daoBatchInfo.setLabel(item.getName());
        daoPackage.setDaoBatchInfo(daoBatchInfo);

        Daoset daoset = new Daoset();
        daoPackage.setDaoset(daoset);

        String fileParam = createFileParam(item);
        Dao dao = createDao(item, fileParam);

        FileGroup fileGroup = new FileGroup();

        List<Bundle> bundles = item.getBundles(DaoImportService.CONTENT_BUNDLE);

        //"http://localhost:8080/xmlui/handle/123456789/8"

        try {
            context.turnOffAuthorisationSystem();

            for (Bundle bundle : bundles) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                    File file = createFile(fileParam, bitstream, context);
                    fileGroup.getFile().add(file);
                }
            }
        } catch (Exception e) {
            context.abort();
            context.restoreAuthSystemState();
            throw new IllegalStateException("Nastala chyba při zápisu metadat isElza k Item " + item + " odesílané do ELZA", e);
        }

        dao.setFileGroup(fileGroup);
        daoset.getDao().add(dao);
        getDaoService().addPackage(daoPackage);

        try {
            MetadataEnum metaData = MetadataEnum.ISELZA;
            List<MetadataValue> metadata = itemService.getMetadata(item, metaData.getSchema(), metaData.getElement(), metaData.getQualifier(), Item.ANY);
            itemService.removeMetadataValues(context, item, metadata);
            itemService.addMetadata(context, item, metaData.getSchema(), metaData.getElement(), metaData.getQualifier(), null, "true");
            context.commit();
        } catch (Exception e) {
            context.abort();
            throw new IllegalStateException("Nastala chyba při zápisu metadat isElza k Item " + item + " odesílané do ELZA", e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    public static Dao createDao(Item item, String fileParam) {
        Dao dao = new Dao();
        dao.setIdentifier(item.getID().toString());
        dao.setLabel(fileParam);
//        dao.setLabel(item.getName());
        return dao;
    }

    public static String createFileParam(Item item) {
        String uriMD = itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "identifier", "uri", Item.ANY);
        int index = uriMD.lastIndexOf("/handle/");
        String fileParam = null;
        if (index > 0) {
            fileParam = uriMD.substring(index + 8);
        }
        return fileParam;
    }

    public static File createFile(String fileParam, Bitstream bitstream, Context context) {
        File file = new File();
        file.setChecksumType(convertStringToChecksumType(bitstream.getChecksumAlgorithm()));
        file.setChecksum(bitstream.getChecksum());
        file.setIdentifier(fileParam + "/" + bitstream.getName());
        file.setSize(bitstream.getSizeBytes());
        file.setDescription(bitstream.getDescription());
        file.setFileName(bitstream.getName());

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

        List<MetadataEnum> techMataData = MetadataEnum.getTechMetaData();
        for (MetadataEnum mt : techMataData) {
            String value = bitstreamService.getMetadataFirstValue(bitstream, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
            switch (mt) {
                case DURATION:
                    file.setDuration(value);
                    break;
                case IMAGEHEIGHT:
                    file.setImageHeight(convertStringToBigInteger(value));
                    break;
                case IMAGEWIDTH:
                    file.setImageWidth(convertStringToBigInteger(value));
                    break;
                case SOURCEXDIMUNIT:
                    file.setSourceXDimensionUnit(convertStringToUnitOfMeasure(value));
                    break;
                case SOURCEXDIMVALUVALUE:
                    file.setSourceXDimensionValue(convertStringToFloat(value));
                    break;
                case SOURCEYDIMUNIT:
                    file.setSourceYDimensionUnit(convertStringToUnitOfMeasure(value));
                    break;
                case SOURCEYDIMVALUVALUE:
                    file.setSourceYDimensionValue(convertStringToFloat(value));
                    break;
            }
        }
        String createdDate = bitstreamService.getMetadataFirstValue(bitstream, Item.ANY, "date", "created", Item.ANY);
        if (createdDate != null) {
            XMLGregorianCalendar xmlGregCalDate = concertStringToXMLGregorianCalendar(createdDate);
            file.setCreated(xmlGregCalDate);
        }
        return file;
    }

    private static DaoRequestsService getDaoRequests() {
        final String url = configurationService.getProperty("elza.base.url") + "services/DaoRequestsService";
        final String username = configurationService.getProperty("elza.base.username");
        final String password = configurationService.getProperty("elza.base.password");
        return getJaxWsRemoteInterface(DaoRequestsService.class, url, username, password);
    }

    private static DaoService getDaoService() {
        final String url = configurationService.getProperty("elza.base.url") + "services/DaoCoreService";
        final String username = configurationService.getProperty("elza.base.username");
        final String password = configurationService.getProperty("elza.base.password");
        return getJaxWsRemoteInterface(DaoService.class, url, username, password);
    }

    private static BigInteger convertStringToBigInteger(String bigNumber) {
        if (StringUtils.isBlank(bigNumber)) {
            return null;
        }
        return BigInteger.valueOf(Long.parseLong(bigNumber));
    }

    private static Float convertStringToFloat(String floatNumber) {
        if (StringUtils.isBlank(floatNumber)) {
            return null;
        }
        return Float.valueOf(floatNumber);
    }

    private static UnitOfMeasure convertStringToUnitOfMeasure(String uomCode) {
        if (StringUtils.isBlank(uomCode)) {
            return null;
        }
        switch (StringUtils.upperCase(uomCode)) {
            case "IN":
                return UnitOfMeasure.IN;
            case "MM":
                return UnitOfMeasure.MM;
        }
        throw new ProcessingException("Kód měrné jednotky " + uomCode + " není podporován.");
    }

    private static XMLGregorianCalendar concertStringToXMLGregorianCalendar(String date) {
        DCDate dcDate = new DCDate(date);
        Date parseDate = dcDate.toDate();

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(parseDate);

        XMLGregorianCalendar xmlGregCalDate = null;
        try {
            xmlGregCalDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (DatatypeConfigurationException e) {
            logger.error("Fail in convert String value of created date to XMLGregorianCalendar", e);
        }
        return xmlGregCalDate;
    }

    /**
     * Konvertuje hodnotu typu String na ChecksumType
     * @param checksumAlgorithm
     * @return
     */
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

    public static void updateItem(Context context, Item item, Did did) {
        try {
            Unitdatestructured unitdatestructured = did.getUnitdatestructured();
            if (unitdatestructured != null) {
                Datesingle datesingle = unitdatestructured.getDatesingle();
                if (datesingle != null) {
                    String date = datesingle.getLocaltype();
                    itemService.setMetadataSingleValue(context, item, MetadataSchema.DC_SCHEMA, "date", "issued", null, date);
                }
            }

            List<Unitid> unitidList = did.getUnitid();
            if (unitidList != null) {
                itemService.clearMetadata(context, item, MetadataSchema.DC_SCHEMA, "identifier", "govdoc", null);
                for (Unitid unitid : unitidList) {
                    itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "identifier", "govdoc", null, unitid.getLocaltype());
                }
            }

            List<Unittitle> unittitleList = did.getUnittitle();
            if (unittitleList != null) {
                itemService.clearMetadata(context, item, MetadataSchema.DC_SCHEMA, "description", "abstract", null);
                for (Unittitle unittitle : unittitleList) {
                    itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "description", "abstract", null, unittitle.getLocaltype());
                }
            }

            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new ProcessingException("Chyba při aktualizaci DAO/Item " + item.getID(), e);
        }
    }
}
