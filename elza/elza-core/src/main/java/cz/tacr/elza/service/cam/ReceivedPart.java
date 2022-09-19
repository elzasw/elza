package cz.tacr.elza.service.cam;

import java.util.List;

import cz.tacr.elza.domain.ApPart;

public class ReceivedPart {

    final ApPart part;

    final List<ReceivedItem> items;

    public ReceivedPart(ApPart part, List<ReceivedItem> items) {
        this.part = part;
        this.items = items;
    }

    public ApPart getPart() {
        return part;
    }

    public List<ReceivedItem> getItems() {
        return items;
    }
}
