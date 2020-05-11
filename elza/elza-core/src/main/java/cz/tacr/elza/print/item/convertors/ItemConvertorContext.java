package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.print.File;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.Structured;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;

import java.util.Locale;

public interface ItemConvertorContext {

    ItemType getItemTypeById(Integer id);

    ItemSpec getItemSpecById(Integer id);

    Record getRecord(ApAccessPoint record);

    File getFile(ArrFile file);

    Structured getStructured(ArrStructuredObject structureData);

    Node getNode(ArrNode arrNode);

    Locale getLocale();


}
