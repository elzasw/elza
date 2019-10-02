package cz.tacr.elza.service;

import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.RulItemType;

import java.util.List;

public interface ByType<T> {

    List<ApItem> findValidItemsByType(T item, RulItemType type);

}
