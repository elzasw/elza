package cz.tacr.elza.drools.service;

import java.util.*;

import javax.annotation.Nullable;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.Structured;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.drools.model.StructObjItem;
import cz.tacr.elza.service.vo.AccessPoint;
import cz.tacr.elza.service.vo.AccessPointMigrate;
import cz.tacr.elza.service.vo.Language;
import cz.tacr.elza.service.vo.Name;
import cz.tacr.elza.service.vo.NameMigrate;
import cz.tacr.elza.service.vo.SimpleItem;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Factory method for the base Drools model objects.
 * This class contains only static methods.
 *
 */
public class ModelFactory {
    /**
     * Vytvoří hodnotu atributu.
     *
     * @param descItem atribut
     * @return vo hodnota atributu
     */
    static public DescItem createDescItem(final ArrDescItem descItem) {
        DescItem item = new DescItem();
        item.setDescItemId(descItem.getItemId());
        item.setType(descItem.getItemType().getCode());
        item.setSpecCode(descItem.getItemSpec() == null ? null : descItem.getItemSpec().getCode());
        item.setDataType(descItem.getItemType().getDataType().getCode());
		item.setUndefined(descItem.isUndefined());
        return item;
    }

    /**
     * Vytvoří level.
     *
     * @param level   level
     * @param version verze levelu
     * @return vo level
     */
    static public Level createLevel(final ArrLevel level, final ArrFundVersion version) {

        Level result = new Level();
        result.setNodeId(level.getNode().getNodeId());

        return result;
    }

    /**
     * Vytvoří hodnoty atributu.
     *
     * @param descItems hodnoty atributu
     * @return seznam vo hodnot atributu
     */
    static public List<DescItem> createDescItems(@Nullable final List<ArrDescItem> descItems,
	        DescItemFactory descItemFactory,
	        StructuredItemRepository itemRepos)
    {
    	if(descItems==null) {
    		return new ArrayList<>();
    	}
        List<DescItem> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            DescItem voDescItem = createDescItem(descItem);
            result.add(voDescItem);

            if (!voDescItem.isUndefined()) {
				ArrData data = descItem.getData();
				if (data.getType() == DataType.STRUCTURED) {
                    ArrDataStructureRef structureRef = HibernateUtils.unproxy(data);
                    ArrStructuredObject structObj = structureRef.getStructuredObject();
                    voDescItem.setStructured(createStructured(structObj, itemRepos));
				} else if (data.getType() == DataType.INT) {
                    ArrDataInteger integer = HibernateUtils.unproxy(data);
                    voDescItem.setInteger(integer.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Create packet for Drools from corresponding object
     * @param structObj
     * @return
     */
    static public Structured createStructured(final ArrStructuredObject structObj, StructuredItemRepository itemRepos) {
        Structured result = new Structured(structObj, itemRepos);
        return result;
    }

    /**
     * Add level and its all parent to the collection
     * @param level Level to be added
     * @param facts Collection where are nodes added
     */
	public static void addLevelWithParents(Level level, Collection<Object> facts) {
		while(level!=null) {
			facts.add(level);
			level = level.getParent();
		}

	}

    /**
     * Vytvoření položek strukturovaného datového typu.
     *
     * @param structuredItems položky pro konverzi
     * @return vytvořené položky
     */
    public static List<StructObjItem> createStructuredItems(@Nullable final List<ArrStructuredItem> structuredItems) {
	    if (structuredItems == null) {
            return Collections.emptyList();
        }

        List<StructObjItem> result = new ArrayList<>(structuredItems.size());
        for (ArrStructuredItem structuredItem : structuredItems) {
            result.add(createStructuredItem(structuredItem));
        }
        return result;
    }

    /**
     * Vytvoření položky strukturovaného datového typu.
     *
     * @param structuredItem položka pro konverzi
     * @return vytvořená položka
     */
	public static StructObjItem createStructuredItem(final ArrStructuredItem structuredItem) {
	    StructObjItem result = new StructObjItem();
        result.setType(structuredItem.getItemType().getCode());
        result.setSpecCode(structuredItem.getItemSpec() == null ? null : structuredItem.getItemSpec().getCode());
        result.setDataType(structuredItem.getItemType().getDataType().getCode());
        if (!structuredItem.isUndefined()) {
            ArrData data = structuredItem.getData();
            if (data.getType() == DataType.INT) {
                ArrDataInteger integer = HibernateUtils.unproxy(data);
                result.setInteger(integer.getValue());
            }
        }
	    return result;
    }

    public static List<SimpleItem> createApItems(final List<ApItem> items) {
	    if (CollectionUtils.isEmpty(items)) {
	        return Collections.emptyList();
        }
        List<SimpleItem> result = new ArrayList<>(items.size());
        for (ApItem item : items) {
            RulItemType itemType = item.getItemType();
            RulItemSpec itemSpec = item.getItemSpec();
            ArrData data = item.getData();
            String value = null;
            if (data != null) {
                if (data.getType() == DataType.ENUM) {
                    value = item.getItemSpec().getName();
                } else {
                    value = data.getFulltextValue();
                }
            }
            SimpleItem fi = new SimpleItem(item.getItemId(), itemType.getCode(), itemSpec == null ? null : itemSpec.getCode(), item.getPosition(), value);
            result.add(fi);
        }
        return result;
    }

    private static Language createApLanguage(final SysLanguage language) {
        if (language == null) {
            return null;
        }
        return new Language(language.getLanguageId(), language.getName(), language.getCode());
    }

    public static AccessPointMigrate createApMigrate(final ApAccessPoint apAcessPoint) {
        return new AccessPointMigrate(null, apAcessPoint.getAccessPointId(), apAcessPoint.getUuid(), null);
    }
}
