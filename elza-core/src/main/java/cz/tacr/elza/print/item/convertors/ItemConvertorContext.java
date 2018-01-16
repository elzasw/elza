package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.Packet;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.party.Party;

public interface ItemConvertorContext {

    ItemType getItemTypeById(Integer id);

    ItemSpec getItemSpecById(Integer id);

    Record getRecord(RegRecord record);

    Party getParty(ParParty party);

    Packet getPacket(ArrPacket packet);

    File getFile(ArrFile file);
}