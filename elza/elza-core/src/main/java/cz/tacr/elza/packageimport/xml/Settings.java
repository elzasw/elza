package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO Settings.
 *
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "settings")
@XmlType(name = "settings")
public class Settings {

    @XmlElements({
            @XmlElement(name = "allow-structure-types", type = SettingStructureTypes.class),
            @XmlElement(name = "fund-views", type = SettingFundViews.class),
            @XmlElement(name = "type-groups", type = SettingTypeGroups.class),
            @XmlElement(name = "setting-base", type = SettingBase.class),
            @XmlElement(name = "record", type = SettingRecord.class),
            @XmlElement(name = "favorite-item-specs", type = SettingFavoriteItemSpecs.class),
            @XmlElement(name = "grid-view", type = SettingGridView.class),
            @XmlElement(name = "fund-issues", type = SettingFundIssues.class),
            @XmlElement(name = "structure-type-settings", type = SettingStructTypeSettings.class),
            @XmlElement(name = "parts-order", type = SettingPartsOrder.class),
            @XmlElement(name = "item-types", type = SettingItemTypes.class),
            @XmlElement(name = "index-search", type = SettingIndexSearch.class),
            @XmlElement(name = "dao-import-level-settings", type = SettingDaoImportLevel.class),
            @XmlElement(name = "menu", type = SettingMenu.class),
    })
    private List<Setting> settings;

    public List<Setting> getSettings() {
        return settings;
    }

    public void setSettings(final List<Setting> settings) {
        this.settings = settings;
    }
}
