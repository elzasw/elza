package cz.tacr.elza.packageimport.xml;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Adaptér pro zapsání ContentDefinition List<=>Mapa.
 *
 * @author Martin Šlapa
 * @since 01.12.2016
 */
public class ContentDefinitionAdapter extends XmlAdapter<ContentDefinitions, Map<String, ContentDefinition>> {

    @Override
    public Map<String, ContentDefinition> unmarshal(final ContentDefinitions contentDefinitions) throws Exception {
        HashMap<String, ContentDefinition> result = new LinkedHashMap<>();
        for (ContentDefinition contentDefinition : contentDefinitions.entries()) {
            result.put(contentDefinition.getCode(), contentDefinition);
        }
        return result;
    }

    @Override
    public ContentDefinitions marshal(final Map<String, ContentDefinition> contentDefinitionMap) throws Exception {
        ContentDefinitions props = new ContentDefinitions();
        for (Map.Entry<String, ContentDefinition> entry : contentDefinitionMap.entrySet()) {
            ContentDefinition contentDefinition = entry.getValue();
            contentDefinition.setCode(entry.getKey());
            props.addEntry(contentDefinition);
        }
        return props;
    }

}
