package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.Structured;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.party.Party;

public interface ItemConvertorContext {

    ItemType getItemTypeById(Integer id);

    ItemSpec getItemSpecById(Integer id);

    Record getRecord(ApRecord record);

    Party getParty(ParParty party);

    File getFile(ArrFile file);

    Structured getStructured(ArrStructureData structureData);
}
