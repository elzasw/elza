package cz.tacr.elza.core.schema;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.OutputCode;

@Component
public class SchemaManager {

    public static final String EAD3_SCHEMA_URL = "http://ead3.archivists.org/schema/";

    private SchemaFactory schemaFactory;

    private Map<String, Schema> schemaMap;

    @PostConstruct
    protected void init() {
        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaMap = new HashMap<>();
    }

    /**
     * Získání schématu podle url schématu
     * @param url schématu
     * @return
     */
    public Schema getSchema(String urlSchema) {
        String fileSchema = schemaUrlToFile(urlSchema);
        if (StringUtils.isEmpty(fileSchema)) {
            throw new SystemException("Validation schema not found", OutputCode.SCHEMA_NOT_FOUND);
        }
        Schema schema = schemaMap.get(fileSchema);
        if (schema == null) {
            try {
                schema = schemaFactory.newSchema(getClass().getResource(fileSchema));
            } catch (SAXException e) {
                throw new SystemException("Failed to create schema", e, BaseCode.INVALID_STATE);
            }
            schemaMap.put(fileSchema, schema);
        }
        return schema;
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
        default:
            return null;
        }
    }

}
