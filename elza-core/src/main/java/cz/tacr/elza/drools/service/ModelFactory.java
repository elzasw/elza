package cz.tacr.elza.drools.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.Structured;

/**
 * Factory method for the base Drools model objects.
 * This class contains only static methods.
 *
 * @author Petr Pytelka
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
     * @param lastVersion
     * @return seznam vo hodnot atributu
     */
    static public List<DescItem> createDescItems(@Nullable final List<ArrDescItem> descItems,
	        DescItemFactory descItemFactory)
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
					ArrDataStructureRef structureRef = (ArrDataStructureRef) descItem.getData();
                    ArrStructureData structureData = structureRef.getStructureData();
                    voDescItem.setStructured(createStructured(structureData));
				} else if (data.getType() == DataType.INT) {
					ArrDataInteger integer = (ArrDataInteger) descItem.getData();
                    voDescItem.setInteger(integer.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Create packet for Drools from corresponding object
     * @param structureData
     * @return
     */
    static public Structured createStructured(final ArrStructureData structureData) {
        Structured result = new Structured();
        result.setValue(structureData.getValue());
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
}
