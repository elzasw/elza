package cz.tacr.elza.ws.core.v1.daoservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Nastavení pro automatický import DAO typu Level
 * 
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dao-level-import", namespace = "dao-import-level")
public class LevelImportSettings {

    @XmlElement(name = "scenario", required = true)
    private String scenarioName;
    // private String parentName;

    @XmlElement(name = "desc-item-type", required = true)
    private String descItemType;

    @XmlElement(name = "desc-prefix", required = false)
    private String descPrefix;

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getDescItemType() {
        return descItemType;
    }

    public void setDescItemType(String descItemType) {
        this.descItemType = descItemType;
    }

    public String getDescPrefix() {
        return descPrefix;
    }

    public void setDescPrefix(String descPrefix) {
        this.descPrefix = descPrefix;
    }

}
