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
    		Set<RulItemType> descItemTypesForPackets,
    		Set<RulItemType> descItemTypesForIntegers,
    		DescItemFactory descItemFactory
    		) 
    {
    	if(descItems==null) {
    		return new ArrayList<>();
    	}
        List<DescItem> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            DescItem voDescItem = createDescItem(descItem);
            result.add(voDescItem);

            if (descItemTypesForPackets.contains(descItem.getItemType())) {
                ArrItemPacketRef packetRef = (ArrItemPacketRef) descItemFactory.getDescItem(descItem).getItem();

                ArrPacket packet = packetRef.getPacket();
                if (packet != null) {
                    voDescItem.setPacket(createPacket(packet));
                }
            } else if (descItemTypesForIntegers.contains(descItem.getItemType())) {
                ArrItemInt integer = (ArrItemInt) descItemFactory.getDescItem(descItem).getItem();
                voDescItem.setInteger(integer.getValue());
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
