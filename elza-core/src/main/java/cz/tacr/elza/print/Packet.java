package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;

public class Packet {

    private final List<NodeId> nodeIds = new ArrayList<>();

    private final NodeLoader nodeLoader;

    private String type;

    private String typeCode;

    private String typeShortcut;

    private String storageNumber;

    private String state;

    public enum FormatType {
        TYPE_WITH_NUMBER, NUMBER_WITH_TYPE, ONLY_TYPE, ONLY_NUMBER
    }

    private Packet(NodeLoader nodeLoader) {
        this.nodeLoader = nodeLoader;
    }

    /**
     * Return formatted packet name
     *
     * Function return typeShortcut and storageNumber
     *
     * @return Return formatted value
     */
    public String formatAsString(FormatType formatType) {
        switch (formatType) {
            case TYPE_WITH_NUMBER:
                return formatTypeWithNumber();
            case NUMBER_WITH_TYPE:
                return formatNumberWithType();
            case ONLY_TYPE:
                return formatOnlyType();
            case ONLY_NUMBER:
                return formatOnlyNumber();
        }
        return "";
    }

    private String formatOnlyNumber() {
        StringJoiner sj = new StringJoiner(" ");
        if (StringUtils.isNotBlank(storageNumber)) {
            sj.add(storageNumber);
        }
        return sj.toString();
    }

    private String formatOnlyType() {
        StringJoiner sj = new StringJoiner(" ");
        if (StringUtils.isNotBlank(typeShortcut)) {
            sj.add(typeShortcut);
        }
        return sj.toString();
    }

    private String formatNumberWithType() {
        StringJoiner sj = new StringJoiner(" ");
        if (StringUtils.isNotBlank(storageNumber)) {
            sj.add(storageNumber);
        }
        if (StringUtils.isNotBlank(typeShortcut)) {
            sj.add(new StringBuilder().append("(").append(typeShortcut).append(")").toString());
        }
        return sj.toString();
    }

    private String formatTypeWithNumber() {
        StringJoiner sj = new StringJoiner(" ");
        if (StringUtils.isNotBlank(typeShortcut)) {
            sj.add(typeShortcut);
        }
        if (StringUtils.isNotBlank(storageNumber)) {
            sj.add(storageNumber);
        }
        return sj.toString();
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getStorageNumber() {
        return storageNumber;
    }

    public void setStorageNumber(final String storageNumber) {
        this.storageNumber = storageNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(final String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeShortcut() {
        return typeShortcut;
    }

    public void setTypeShortcut(final String typeShortcut) {
        this.typeShortcut = typeShortcut;
    }

    public IteratorNodes getNodes() {
        Iterator<NodeId> nodeIdIterator = nodeIds.iterator();
        return new IteratorNodes(nodeLoader, nodeIdIterator);
    }

    void addNodeId(NodeId nodeId) {
        Validate.notNull(nodeId);
        nodeIds.add(nodeId);
    }

    public static Packet newInstance(ArrPacket arrPacket, RuleSystem ruleSystem, NodeLoader nodeLoader) {
        Packet packet = new Packet(nodeLoader);
        packet.setStorageNumber(arrPacket.getStorageNumber());
        packet.setState(arrPacket.getState().name());
        if (arrPacket.getPacketTypeId() != null) {
            RulPacketType packetType = ruleSystem.getPacketTypeById(arrPacket.getPacketTypeId());
            packet.setType(packetType.getName());
            packet.setTypeCode(packetType.getCode());
            packet.setTypeShortcut(packetType.getShortcut());
        }
        return packet;
    }
}
