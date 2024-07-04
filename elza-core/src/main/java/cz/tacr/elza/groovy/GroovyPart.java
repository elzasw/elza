package cz.tacr.elza.groovy;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

public class GroovyPart {

    /*!
     * \brief Static data provider
     */
    final StaticDataProvider sdp;

    /**
     * Typ archivní entity.
     */
    private final ApType apType;

    /**
     * Jedná se o preferovanou část archivní entity.
     */
    private boolean preferred;

    /**
     * Typ části archivní entity.
     */
    private RulPartType partType;

    /**
     * Itemy části archivní entity.
     */
    private GroovyItems items;

    /**
     * Související části archivní entity.
     */
    private List<GroovyPart> children;

    public GroovyPart(final StaticDataProvider sdp,
                      final int apTypeId,
                      final int partTypeId,
                      final boolean preferred,
                      final GroovyItems items,
                      final List<GroovyPart> children) {
        this.sdp = sdp;
        this.apType = sdp.getApTypeById(apTypeId);
        Validate.notNull(apType);
        
        this.partType = sdp.getPartTypeById(partTypeId);
        Validate.notNull(partType);

        this.preferred = preferred;
        this.items = items;
        this.children = children;
    }

    public String getAeType() {
        return apType.getCode();
    }

    public boolean isPreferred() {
        return preferred;
    }

    public RulPartType getPartType() {
        return partType;
    }

    public String getPartTypeCode() {
        return partType.getCode();
    }

    public List<GroovyItem> getItems(@NotNull String itemTypeCode) {
        // validate if valid code
        ItemType itemType = sdp.getItemTypeByCode(itemTypeCode);
        if (itemType == null) {
            throw new BusinessException("Item type code not found: " + itemTypeCode, BaseCode.PROPERTY_IS_INVALID);
        }
        return items.getItems(itemType);
    }

    public List<GroovyItem> getItems() {
        return items.getAllItems();
    }

    public List<GroovyPart> getChildren() {
        return children;
    }

    public enum PreferredFilter {
        YES, NO, ALL
    }
}
