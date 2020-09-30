package cz.tacr.elza.print.part;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.ApIndex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;

public class Part {

    private final int partId;

    private final int accessPointId;

    private final String value;

    private final Integer parentPartId;

    private final PartType partType;

    private List<Item> items = new ArrayList<>();

    private List<Part> parts = Collections.emptyList();

    public Part(ApPart apPart, StaticDataProvider staticData, ApIndex index) {
        this.partId = apPart.getPartId();
        this.accessPointId = apPart.getAccessPointId();
        this.value = index != null ? index.getValue() : null;
        this.parentPartId = apPart.getParentPart() != null ? apPart.getParentPart().getPartId() : null;
        this.partType = new PartType(staticData.getPartTypeById(apPart.getPartTypeId()));
    }

    public List<Item> getItems() {
        return items;
    };

    /**
     * @param typeCodes seznam kódů typů atributů.
     * @return vrací se seznam hodnot těchto atributů, řazeno dle ap_item.position
     */
    public List<Item> getItems(final Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        if (items == null || typeCodes.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            return typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    /**
     * Return list of items with given specification
     *
     * @param typeCode Code of the item
     * @param specCode Code of specificaion
     * @return
     */
    public List<Item> getItemsWithSpec(final String typeCode, final String specCode) {
        Validate.notNull(typeCode);
        Validate.notNull(specCode);

        if (items == null) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            if (!typeCode.equals(tc)) {
                return false;
            }
            ItemSpec is = item.getSpecification();
            return is != null && specCode.equals(is.getCode());
        }).collect(Collectors.toList());
    }

    /**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě
     * hodnot typů uvedených ve vstupu metody, řazeno dle ap_item.position
     *
     * @param typeCodes seznam kódu typů atributů
     * @return seznam všech hodnot atributů kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getItemsWithout(final Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        if (items == null) {
            return Collections.emptyList();
        }

        return items.stream().filter(item -> {
            String tc = item.getType().getCode();
            return !typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    public Item getSingleItem(String typeCode) {
        Validate.notEmpty(typeCode);
        if (items == null) {
            return null;
        }
        Item found = null;

        for (Item item : items) {
            if (typeCode.equals(item.getType().getCode())) {
                // check if item already found
                if (found != null) {
                    throw new IllegalStateException("Multiple items with same code exists: " + typeCode);
                }
                found = item;
            }
        }
        return found;
    }

    public String getSingleItemValue(String itemTypeCode) {
        Item found = getSingleItem(itemTypeCode);
        if (found != null) {
            return found.getSerializedValue();
        }
        return StringUtils.EMPTY;
    }

    public PartType getPartType() {
        return partType;
    }

    public int getPartId() {
        return partId;
    }

    public int getAccessPointId() {
        return accessPointId;
    }

    public String getValue() {
        return value;
    }

    public Integer getParentPartId() {
        return parentPartId;
    }

    public void setParts(final List<Part> subParts) {
        parts = subParts;
    }

    public List<Part> getParts() {
        return parts;
    }

    public void addItem(Item item) {
        this.items.add(item);
    }

}
