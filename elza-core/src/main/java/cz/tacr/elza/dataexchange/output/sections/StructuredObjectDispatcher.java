package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructureData;

public class StructuredObjectDispatcher implements LoadDispatcher<ArrStructureData> {

    private final SectionOutputStream os;

    private final RuleSystem ruleSystem;

    private final ArrFund fund;

    private ArrStructureData structuredData;

    public StructuredObjectDispatcher(SectionOutputStream os, RuleSystem ruleSystem, ArrFund fund) {
        this.os = os;
        this.ruleSystem = ruleSystem;
        this.fund = Validate.notNull(fund);
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(ArrStructureData result) {
        Validate.isTrue(structuredData == null);
        structuredData = result;
    }

    @Override
    public void onLoadEnd() {
        // TODO: Finish implementation
        /*
        // update packet type reference
        if (structuredData.getStructureDataId() != null) {
            RulPacketType packetType = ruleSystem.getPacketTypeById(packet.getPacketTypeId());
            Validate.notNull(packetType);
            packet.setPacketType(packetType);
        }*/
        // update fund reference
        structuredData.setFund(fund);

        os.addStructuredObject(structuredData);
    }
}
