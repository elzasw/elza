package cz.tacr.elza.metadataconstants;

import org.apache.commons.lang.StringUtils;

import javax.ws.rs.ProcessingException;

/**
 * Dle označení metadat vrátí element qualifier pole metadat a shordId schématu metadat
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 05.07.2019.
 */
public class MetadataConstantService {

    public static String[] getMetaData(String metDataCode) {
        return(convertMetaData(StringUtils.upperCase(metDataCode)));
    }

    public static String[] getTechMetaData() {
        return(techMetaData());
    }

    private static String[] convertMetaData (String metDataCode) {
        // Vrátí MetadataSchema, Element, qualifier
        switch (metDataCode) {
            case "ISELZA":
                return new String[]{"tacr", "isElza", null};
            case "TECH1":
                return new String[]{"tacr", "format", null};
            case "TECH2":
                return new String[]{"tacr", "type", null};
            default:
                throw new ProcessingException("Pole metadat " + metDataCode + " Není podporováno.");
        }
    }

    private static String[] techMetaData()  {
        // Vrátí řetězec s názvy technických metadat
        String[] mataDataCodes = {
                "TECH1",
                "TECH2"};
        return mataDataCodes;
    }
}
