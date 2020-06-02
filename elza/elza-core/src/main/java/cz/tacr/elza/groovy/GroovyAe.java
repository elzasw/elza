package cz.tacr.elza.groovy;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.service.GroovyService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.*;

public class GroovyAe {

    /**
     * Kód typu archivní entity.
     */
    private String aeType;

    /**
     * Seznam částí archivní entity
     */
    private List<GroovyPart> parts;

    public GroovyAe(final String aeType, final List<GroovyPart> parts) {
        this.parts = parts;
        this.aeType = aeType;
    }

    public String getAeType() {
        return aeType;
    }

    public List<GroovyPart> getParts() {
        return parts;
    }

    public boolean isChildOf(final String aeTypeCode) {
        Validate.notNull(aeTypeCode, "Kód typu archivní entity musí být vyplněn");
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        ApType apType = sdp.getApTypeByCode(aeTypeCode);
        List<ApType> apTypes = sdp.getApTypes();
        List<ApType> treeAeTypes = findTreeAeTypes(apTypes, apType.getApTypeId());
        for (ApType treeAeType : treeAeTypes) {
            if (treeAeType.getCode().equalsIgnoreCase(aeType)) {
                return true;
            }
        }
        return false;
    }

    private List<ApType> findTreeAeTypes(final List<ApType> apTypes, final Integer id) {
        ApType parent = getById(apTypes, id);
        Set<ApType> result = new HashSet<>();
        result.add(parent);
        for (ApType item : apTypes) {
            if (parent.equals(item.getParentApType())) {
                result.addAll(findTreeAeTypes(apTypes, item.getApTypeId()));
            }
        }
        return new ArrayList<>(result);
    }

    private ApType getById(final List<ApType> apTypes, final Integer id) {
        for (ApType apType : apTypes) {
            if (apType.getApTypeId().equals(id)) {
                return apType;
            }
        }
        return null;
    }

    @Nullable
    public GroovyPart findPreferPart(final String partTypeCode) {
        Validate.notNull(partTypeCode, "Musí být vyplněn typ části");
        if (CollectionUtils.isEmpty(parts)) {
            return null;
        }
        for (GroovyPart part : parts) {
            if (part.getPartTypeCode().equals(partTypeCode) && part.isPreferred()) {
                return part;
            }
        }
        return null;
    }

    public List<GroovyAe> findParents(final String itemTypeStruct) {
        Validate.notNull(itemTypeStruct, "Kód typu hodnoty atributu musí být vyplněna");
        ItemType itemType = StaticDataProvider.getInstance().getItemTypeByCode(itemTypeStruct);

        if (itemType.getDataType() != DataType.RECORD_REF) {
            throw new IllegalArgumentException("Kód typu hodnoty atributu musí být odkaz na jinou AE");
        }

        if (CollectionUtils.isEmpty(parts)) {
            return Collections.emptyList();
        }

        for (GroovyPart part : parts) {
            List<GroovyItem> items = part.getItems(itemTypeStruct);
            if (CollectionUtils.isNotEmpty(items)) {
                GroovyItem groovyItem = items.get(0);
                Integer recordId = groovyItem.getIntValue();
                return GroovyService.findParentAe(recordId, itemType);
            }
        }
        return Collections.emptyList();
    }
}
