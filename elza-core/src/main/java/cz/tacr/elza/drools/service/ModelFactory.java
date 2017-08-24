package cz.tacr.elza.drools.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.Packet;
import org.apache.commons.lang.BooleanUtils;

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
        item.setUndefined(BooleanUtils.isTrue(descItem.isUndefined()));
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
                                                 Set<RulItemType> descItemTypesForPackets,
                                                 Set<RulItemType> descItemTypesForIntegers,
                                                 DescItemFactory descItemFactory,
                                                 final boolean lastVersion)
    {
    	if(descItems==null) {
    		return new ArrayList<>();
    	}
        List<DescItem> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            DescItem voDescItem = createDescItem(descItem);
            result.add(voDescItem);

            if (!voDescItem.isUndefined()) {
                if (descItemTypesForPackets.contains(descItem.getItemType())) {
                    ArrDataPacketRef packetRef = lastVersion ? (ArrDataPacketRef) descItem.getData() : (ArrDataPacketRef) descItemFactory.getDescItem(descItem).getData();
                    ArrPacket packet = packetRef.getPacket();
                    voDescItem.setPacket(createPacket(packet));
                } else if (descItemTypesForIntegers.contains(descItem.getItemType())) {
                    ArrDataInteger integer = lastVersion ? (ArrDataInteger) descItem.getData() : (ArrDataInteger) descItemFactory.getDescItem(descItem).getData();
                    voDescItem.setInteger(integer.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Create packet for Drools from corresponding object
     * @param packet
     * @return
     */
    static public Packet createPacket(final ArrPacket packet) {

        Packet result = new Packet();
        result.setStorageNumber(packet.getStorageNumber());
        result.setState(packet.getState());

        if (packet.getPacketType() != null) {
            RulPacketType packetType = packet.getPacketType();
            Packet.VOPacketType voPacketType = new Packet.VOPacketType();
            voPacketType.setCode(packetType.getCode());
            voPacketType.setName(packetType.getName());
            voPacketType.setShortcut(packetType.getShortcut());
            result.setPacketType(voPacketType);
        }
        return result;
    }

    /**
     * Add level and its all parent to the collection
     * @param level Level to be added
     * @param facts Collection where are nodes added
     */
	public static void addAll(Level level, Collection<Object> facts) {
		while(level!=null) {
			facts.add(level);
			level = level.getParent();
		}

	}
}
