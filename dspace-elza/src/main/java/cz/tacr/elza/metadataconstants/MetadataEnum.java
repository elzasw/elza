package cz.tacr.elza.metadataconstants;

import java.util.LinkedList;
import java.util.List;

/**
 * Seznam metadat.
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 05.07.2019.
 */
public enum MetadataEnum {

    ISELZA("tacr", "isElza", null, false),
    ELZADIDID("tacr", "elzaDidId", null, false),
    DURATION("tacr", "duration", null, true),
    IMAGEHEIGHT("tacr", "imageHeight", null, true),
    IMAGEWIDTH("tacr", "imageWidth", null, true),
    SOURCEXDIMUNIT("tacr", "sourceXdimUnit", null, true),
    SOURCEXDIMVALUVALUE("tacr", "sourceXdimValue", null, true),
    SOURCEYDIMUNIT("tacr", "sourceXdimUnit", null, true),
    SOURCEYDIMVALUVALUE("tacr", "sourceXdimValue", null, true);

    private String schema;
    private String element;
    private String qualifier;
    private boolean techMD;

    MetadataEnum(String schema, String element, String qualifier, boolean techMD) {
        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
        this.techMD = techMD;
    }

    public static List<MetadataEnum> getTechMetaData() {
        List<MetadataEnum> techMD = new LinkedList<>();
        for (MetadataEnum md : values()) {
            if (md.isTechMD()) {
                techMD.add(md);
            }
        }
        return techMD;
    }

    public String getSchema() {
        return schema;
    }

    public String getElement() {
        return element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public boolean isTechMD() {
        return techMD;
    }
}
