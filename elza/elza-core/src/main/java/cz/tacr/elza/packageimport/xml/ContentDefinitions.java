package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class ContentDefinitions {

    @XmlElement(name = "content-definition")
    private List<ContentDefinition> entries = new ArrayList<>();

    List<ContentDefinition> entries() {
        return Collections.unmodifiableList(entries);
    }

    void addEntry(ContentDefinition entry) {
        entries.add(entry);
    }

}
