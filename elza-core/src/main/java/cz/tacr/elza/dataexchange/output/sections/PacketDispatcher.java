package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;

public class PacketDispatcher implements LoadDispatcher<ArrPacket> {

    private final SectionOutputStream os;

    private final RuleSystem ruleSystem;

    private final ArrFund fund;

    private ArrPacket packet;

    public PacketDispatcher(SectionOutputStream os, RuleSystem ruleSystem, ArrFund fund) {
        this.os = os;
        this.ruleSystem = ruleSystem;
        this.fund = Validate.notNull(fund);
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(ArrPacket result) {
        Validate.isTrue(packet == null);
        packet = result;
    }

    @Override
    public void onLoadEnd() {
        // update packet type reference
        if (packet.getPacketTypeId() != null) {
            RulPacketType packetType = ruleSystem.getPacketTypeById(packet.getPacketTypeId());
            Validate.notNull(packetType);
            packet.setPacketType(packetType);
        }
        // update fund reference
        packet.setFund(fund);

        os.addPacket(packet);
    }
}
