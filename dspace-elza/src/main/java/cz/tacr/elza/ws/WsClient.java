package cz.tacr.elza.ws;

import cz.tacr.elza.context.ContextUtils;
import cz.tacr.elza.daoimport.service.DaoImportService;
import cz.tacr.elza.metadataconstants.MetadataEnum;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.core.v1.DaoService;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.feature.FastInfosetFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 03.07.2019.
 */
public class WsClient {

    private static final Logger logger = LoggerFactory.getLogger(WsClient.class);
    private static ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
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

    public static void sendItemToElza(Item item) {

        DaoPackage daoPackage = new DaoPackage();
        daoPackage.setIdentifier(item.getID().toString());

        Collection collection = item.getOwningCollection();
        String fundId = collectionService.getMetadataFirstValue(collection, MetadataSchema.DC_SCHEMA, "description", "abstract", Item.ANY);
        daoPackage.setFundIdentifier(fundId);
        daoPackage.setFundIdentifier("393b642a-4f5a-467f-aae4-3d129f04a6cb");
        daoPackage.setRepositoryIdentifier(configurationService.getProperty("elza.repositoryCode"));

        String daoId = itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "description", null, null);
        DaoBatchInfo daoBatchInfo = new DaoBatchInfo();
        daoBatchInfo.setIdentifier(daoId);
        daoBatchInfo.setLabel(daoId);
        daoPackage.setDaoBatchInfo(daoBatchInfo);

        Daoset daoset = new Daoset();
        daoPackage.setDaoset(daoset);
        Dao dao = new Dao();
        dao.setIdentifier(item.getID().toString());

        FileGroup fileGroup = new FileGroup();

        List<Bundle> bundles = item.getBundles(DaoImportService.CONTENT_BUNDLE);

        String uriMD = itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "identifier", "uri", Item.ANY);
        int index = uriMD.lastIndexOf("/handle/");
        String fileParam = null;
        if (index > 0) {
            fileParam = uriMD.substring(index + 8);
            dao.setLabel(fileParam);
        }
        //"http://localhost:8080/xmlui/handle/123456789/8"
        for (Bundle bundle : bundles) {
            for (Bitstream bitstream : bundle.getBitstreams()) {
                File file = createFile(fileParam, bitstream);
                List<MetadataEnum> techMataData = MetadataEnum.getTechMetaData();
                DSpaceObjectService dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(item);
                for (MetadataEnum mt : techMataData) {
                    String value = dSpaceObjectService.getMetadataFirstValue(bitstream, mt.getSchema(), mt.getElement(), mt.getQualifier(), Item.ANY);
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
                            file.setSourceXDimensionValue(Float.valueOf(value));
                            break;
                        case SOURCEYDIMUNIT:
                            file.setSourceYDimensionUnit(convertStringToUnitOfMeasure(value));
                            break;
                        case SOURCEYDIMVALUVALUE:
                            file.setSourceYDimensionValue(Float.valueOf(value));
                            break;
                    }
                }
                String createdDate = dSpaceObjectService.getMetadataFirstValue(bitstream, Item.ANY, "date", "created", Item.ANY);
                if (createdDate != null) {
                    XMLGregorianCalendar xmlGregCalDate = concertStringToXMLGregorianCalendar(createdDate);
                    file.setCreated(xmlGregCalDate);
                }
                fileGroup.getFile().add(file);
            }
        }

        dao.setFileGroup(fileGroup);
        daoset.getDao().add(dao);
        getDaoService().addPackage(daoPackage);

        Context context = null;
        try {
            context = ContextUtils.createContext();
            context.turnOffAuthorisationSystem();

            MetadataEnum metaData = MetadataEnum.ISELZA;
            List<MetadataValue> metadata = itemService.getMetadata(item, metaData.getSchema(), metaData.getElement(), metaData.getQualifier(), Item.ANY);
            itemService.removeMetadataValues(context, item, metadata);
            itemService.addMetadata(context, item, metaData.getSchema(), metaData.getElement(), metaData.getQualifier(), null, "true");
            context.complete();
        } catch (Exception e) {
            context.abort();
            throw new IllegalStateException("Nastala chyba při zápisu metadat isElza k Item " + item + " odesílané do ELZA", e);
        } finally {
            if (context != null) {
                context.restoreAuthSystemState();
            }
        }
    }

    private static File createFile(String fileParam, Bitstream bitstream) {
        File file = new File();
        file.setChecksum(bitstream.getChecksum());
        file.setIdentifier(fileParam + "/" + bitstream.getName());
        file.setSize(bitstream.getSizeBytes());

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

    private static XMLGregorianCalendar concertStringToXMLGregorianCalendar(String date) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date parseDate = null;
        try {
            date = date.replace("T", " ");
            date = date.replace("Z", "");
            parseDate = format.parse(date);
        } catch (ParseException e) {
            logger.error("Fail in convert String value of created date to XMLGregorianCalendar", e);
        }

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
}
