package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO Settings.
 *
 * @author Martin Šlapa
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
    })
    private List<Setting> settings;

    public List<Setting> getSettings() {
        return settings;
    }

    public void setSettings(final List<Setting> settings) {
        this.settings = settings;
    }
}
