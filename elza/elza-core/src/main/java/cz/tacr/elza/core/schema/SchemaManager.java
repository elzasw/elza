package cz.tacr.elza.core.schema;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.OutputCode;

@Component
public class SchemaManager {

    private static Logger logger = LoggerFactory.getLogger(SchemaManager.class);

    public static final String EAD3_SCHEMA_URL = "http://ead3.archivists.org/schema/";
    public static final String CAM_SCHEMA_URL = "http://cam.tacr.cz/2019";

    private SchemaFactory schemaFactory;

    /**
     * Schema URL to schema
     */
    private Map<String, Schema> schemaMap = new HashMap<>();

    @PostConstruct
    protected void init() {
        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    }

    /**
     * Získání schématu podle url schématu
     * @param url schématu
     * @return
     */
    public Schema getSchema(final String urlSchema) {
        return schemaMap.computeIfAbsent(urlSchema,
                                         u -> loadSchema(u));
    }

    private Schema loadSchema(String urlSchema) {
        logger.debug("Loading schema: {}", urlSchema);

        String fileSchema = schemaUrlToFile(urlSchema);
        if (StringUtils.isEmpty(fileSchema)) {
            logger.error("Schema not found: {}", urlSchema);
            throw new SystemException("Validation schema not found", OutputCode.SCHEMA_NOT_FOUND)
                    .set("schema", urlSchema);
        }
        try {
            URL rsrc = getClass().getResource(fileSchema);
            if (rsrc == null) {
                logger.error("Failed to read schema from resource: {}", fileSchema);
                throw new SystemException("Failed to read schema", BaseCode.INVALID_STATE)
                        .set("schema", urlSchema)
                        .set("file", fileSchema);
            }
            Schema schema = schemaFactory.newSchema(rsrc);
            return schema;
        } catch (SAXException e) {
            logger.error("Failed to read schema: {}", urlSchema, e);
            throw new SystemException("Failed to create schema", e, BaseCode.INVALID_STATE)
                    .set("schema", urlSchema);
        }
    }

    /**
     * Získání cesty k souboru podle URL schématu
     * @param url schématu
     * @return cesta k souboru nebo null
     */
    public String schemaUrlToFile(String url) {
        switch (url) {
        case EAD3_SCHEMA_URL:
            return "/schema/ead3.xsd";
        case CAM_SCHEMA_URL:
            return "/cam/cam-2019.xsd";
        default:
            return null;
        }
    }

}
