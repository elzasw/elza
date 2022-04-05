package cz.tacr.elza.groovy;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class GroovyUtils {

    public static GroovyAppender createAppender(final GroovyPart p) {
        return new GroovyAppender(p);
    }

    public static GroovyAppender createAppender() {
        return new GroovyAppender();
    }

    public static GroovyUnitdateFormatter formatUnitdate(final GroovyItem from, final GroovyItem to) {
        return new GroovyUnitdateFormatter(from, to);
    }

    @Nullable
    public static GroovyAe findFirstAeBy(final Collection<GroovyAe> aes, final String typeCode, final String specCode) {
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        sdp.getItemTypeByCode(typeCode);
        sdp.getItemSpecByCode(specCode);

        if (CollectionUtils.isEmpty(aes)) {
            return null;
        }
        for (GroovyAe ae : aes) {
            for (GroovyPart part : ae.getParts()) {
                List<GroovyItem> items = part.getItems(typeCode);
                if (items.size() > 0) {
                    GroovyItem groovyItem = items.get(0);
                    if (Objects.equals(groovyItem.getSpecCode(), specCode)) {
                        return ae;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static GroovyItem findItemByPartContains(final GroovyAe groovyAe,
                                                    final String partTypeCode,
                                                    final String containItemType,
                                                    final String containItemSpec,
                                                    final String itemType) {
        Validate.notNull(groovyAe, "Nebyla předána entita pro vyhledání");
        Validate.notNull(partTypeCode, "Nebyla předán typ části entity");
        //StaticDataProvider sdp = StaticDataProvider.getInstance();
        //sdp.getItemTypeByCode(containItemType);
        //sdp.getItemSpecByCode(containItemSpec);
        //sdp.getItemTypeByCode(itemType);

        for (GroovyPart part : groovyAe.getParts()) {
            if (part.getPartTypeCode().equals(partTypeCode)) {
                List<GroovyItem> items = part.getItems(containItemType);
                for (GroovyItem containItem : items) {
                    if (Objects.equals(containItem.getTypeCode(), containItemType)
                            && Objects.equals(containItem.getSpecCode(), containItemSpec)) {
                        List<GroovyItem> findItems = part.getItems(itemType);
                        for (GroovyItem findItem : findItems) {
                            if (Objects.equals(findItem.getTypeCode(), itemType)) {
                                return findItem;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static GroovyItem findFirstItem(final GroovyAe groovyAe, final String partTypeCode, final GroovyPart.PreferredFilter filter, final String itemType) {
        Validate.notNull(groovyAe, "Nebyla předána entita pro vyhledání");
        Validate.notNull(partTypeCode, "Nebyla předán typ části entity");
        Validate.notNull(filter, "Nebyl předán filter preferované části");

        for (GroovyPart part : groovyAe.getParts()) {
            if (filter == GroovyPart.PreferredFilter.ALL
                || filter == GroovyPart.PreferredFilter.NO && !part.isPreferred()
                || filter == GroovyPart.PreferredFilter.YES && part.isPreferred()
            ) {
                if (part.getPartTypeCode().equals(partTypeCode)) {
                    List<GroovyItem> items = part.getItems(itemType);
                    if (items.size() > 0) {
                        return items.get(0);
                    }
                }
            }
        }
        return null;
    }
    
    @Nullable
    public static List<GroovyItem> findAllItems(final GroovyAe groovyAe, final String partTypeCode, final GroovyPart.PreferredFilter filter, final String itemType) {
        Validate.notNull(groovyAe, "Nebyla předána entita pro vyhledání");
        Validate.notNull(partTypeCode, "Nebyla předán typ části entity");
        Validate.notNull(filter, "Nebyl předán filter preferované části");
        List<GroovyItem> groovyItems = new ArrayList<>();

        for (GroovyPart part : groovyAe.getParts()) {
            if (filter == GroovyPart.PreferredFilter.ALL
                || filter == GroovyPart.PreferredFilter.NO && !part.isPreferred()
                || filter == GroovyPart.PreferredFilter.YES && part.isPreferred()
            ) {
                if (part.getPartTypeCode().equals(partTypeCode)) {
                    List<GroovyItem> items = part.getItems(itemType);
                    groovyItems.addAll(items);
                }
            }
        }
        return groovyItems;
    }

    public static boolean hasParent(Integer typeId, String parentCode) {
        Validate.notNull(typeId, "Nebyla předán typ entity");
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        ApType itemType = sdp.getApTypeById(typeId);
        if (itemType != null) {
            Integer parentItemTypeId = itemType.getParentApTypeId();
            ApType parentItemType = sdp.getApTypeById(parentItemTypeId);
    
            return Objects.equals(parentItemType.getCode(), parentCode);
        }
        return false;
    }

}
