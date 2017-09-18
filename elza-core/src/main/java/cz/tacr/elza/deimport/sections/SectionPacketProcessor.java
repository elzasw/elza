package cz.tacr.elza.deimport.sections;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.sections.context.ContextSection;
import cz.tacr.elza.deimport.sections.context.SectionsContext;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.schema.v2.Packet;
import cz.tacr.elza.schema.v2.PacketState;

/**
 * Implementation is not thread-safe.
 */
public class SectionPacketProcessor implements ItemProcessor {

    private final ContextSection currentSection;

    private RulPacketType packetType;

    public SectionPacketProcessor(SectionsContext context) {
        this.currentSection = context.getCurrentSection();
    }

    @Override
    public void process(Object item) {
        Packet packet = (Packet) item;
        prepareCachedReferences(packet);
        validatePacket(packet);
        processPacket(packet);
    }

    protected void prepareCachedReferences(Packet item) {
        if (StringUtils.isNotEmpty(item.getT())) {
            packetType = currentSection.getRuleSystem().getPacketTypeByCode(item.getT());
        } else {
            packetType = null;
        }
    }

    private void validatePacket(Packet item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("Package id is not set");
        }
        if (StringUtils.isBlank(item.getN())) {
            throw new DEImportException("Package name is not set, packetId:" + item.getId());
        }
        if (StringUtils.isNotEmpty(item.getT()) && packetType == null) {
            throw new DEImportException("Package type not found, packetId:" + item.getId() + ", code:" + item.getT());
        }
    }

    private void processPacket(Packet item) {
        ArrPacket entity = new ArrPacket();
        entity.setFund(currentSection.getFund());
        entity.setPacketType(packetType);
        entity.setState(converPacketState(item.getS()));
        entity.setStorageNumber(item.getN());
        currentSection.addPacket(entity, item.getId());
    }

    public static ArrPacket.State converPacketState(PacketState state) {
        if (state == null) {
            return ArrPacket.State.OPEN;
        }
        switch (state) {
            case O:
                return ArrPacket.State.OPEN;
            case C:
                return ArrPacket.State.CLOSED;
            case D:
                return ArrPacket.State.CANCELED;
            default:
                throw new IllegalStateException("Uknown packet state:" + state);
        }
    }
}
