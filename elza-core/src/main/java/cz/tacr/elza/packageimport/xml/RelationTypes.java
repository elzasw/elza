package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO typ vztahu - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "relation-types")
@XmlType(name = "relation-types")
public class RelationTypes {

    @XmlElement(name = "relation-type", required = true)
    private List<RelationType> relationTypes;

    public List<RelationType> getRelationTypes() {
        return relationTypes;
    }

    public void setRelationTypes(final List<RelationType> relationTypes) {
        this.relationTypes = relationTypes;
    }
}
