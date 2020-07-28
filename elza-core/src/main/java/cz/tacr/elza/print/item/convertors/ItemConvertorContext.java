package cz.tacr.elza.print.item.convertors;

import java.util.Locale;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.Structured;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.party.Party;

public interface ItemConvertorContext {

    ItemType getItemTypeById(Integer id);

    ItemSpec getItemSpecById(Integer id);

    Record getRecord(ApAccessPoint record);

    Party getParty(ParParty party);

    File getFile(ArrFile file);

    Structured getStructured(ArrStructuredObject structureData);

    Node getNode(ArrNode arrNode);

    Locale getLocale();


}
