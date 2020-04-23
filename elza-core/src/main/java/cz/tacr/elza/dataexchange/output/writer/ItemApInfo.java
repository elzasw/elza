package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.ApItem;

import java.util.Collection;
import java.util.Map;

public interface ItemApInfo {

    Map<Integer, Collection<ApItem>> getItems();

    void setItems(Map<Integer,Collection<ApItem>> items);
}
