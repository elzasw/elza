package cz.tacr.elza.metadataconstants;

import org.apache.commons.lang.StringUtils;

import javax.ws.rs.ProcessingException;

/**
 * Dle označení metadat vrátí element qualifier pole metadat a shordId schématu metadat
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 05.07.2019.
 */
public class MetadataConstantService {

    public static String[] getMetData(String metDataName) {
        return(convertMetaData(StringUtils.upperCase(metDataName)));
    }

    private static String[] convertMetaData (String metDataName) {
        // Vrátí MetadataSchema, Element, qualifier
        switch (metDataName) {
            case "ISELZA":
                return new String[]{"tacr", "isElza", null};
            default:
                throw new ProcessingException("Pole metadat " + metDataName + " Není podporováno.");
        }
    }
}
