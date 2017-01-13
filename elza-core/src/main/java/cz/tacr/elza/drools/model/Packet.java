package cz.tacr.elza.drools.model;

import cz.tacr.elza.domain.ArrPacket;

/**
 * Objekt obalu atributu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 08.12.2015
 */
public class Packet {

    private String storageNumber;
    private ArrPacket.State state;
    private VOPacketType packetType;

    public String getStorageNumber() {
        return storageNumber;
    }

    public void setStorageNumber(final String storageNumber) {
        this.storageNumber = storageNumber;
    }

    public ArrPacket.State getState() {
        return state;
    }

    public void setState(final ArrPacket.State state) {
        this.state = state;
    }

    public VOPacketType getPacketType() {
        return packetType;
    }

    public void setPacketType(final VOPacketType packetType) {
        this.packetType = packetType;
    }


    /**
     * Typ obalu atributu.
     */
    public static class VOPacketType {

        private String code;
        private String name;
        private String shortcut;

        public VOPacketType() {
        }

        public VOPacketType(final String code, final String name, final String shortcut) {
            this.code = code;
            this.name = name;
            this.shortcut = shortcut;
        }

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getShortcut() {
            return shortcut;
        }

        public void setShortcut(final String shortcut) {
            this.shortcut = shortcut;
        }
    }
}
