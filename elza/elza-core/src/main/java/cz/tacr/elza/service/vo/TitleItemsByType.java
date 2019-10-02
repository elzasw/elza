package cz.tacr.elza.service.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cz.tacr.elza.domain.vo.DescItemValues;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.domain.vo.TitleValues;

/**
 * Collection of items
 * 
 * Collection has fast lookup by itemTypeId
 *
 */
public class TitleItemsByType {
    /**
     * Map with items by itemTypeId;
     */
    Map<Integer, TitleValues> items = new HashMap<>();

    public void addItem(Integer itemTypeId, TitleValue titleValue) {
        TitleValues values = items.computeIfAbsent(itemTypeId,
                                                   v -> new TitleValues());
        values.addValue(titleValue);
    }

    public TitleValues getTitles(Integer itemTypeId) {
        return items.get(itemTypeId);
    }

    /**
     * Return collection of values for given type
     * 
     * @param itemTypeId
     * @return
     */
    public List<String> getValues(Integer itemTypeId) {
        TitleValues titleValues = items.get(itemTypeId);
        if (titleValues == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (TitleValue titleValue : titleValues.getValues()) {
            result.add(titleValue.getValue());
        }
        return result;
    }

    public Map<Integer, DescItemValues> toDescItemValues() {
        Map<Integer, DescItemValues> descItemValuesMap = new HashMap<>(items.size());
        for (Entry<Integer, TitleValues> entry : items.entrySet()) {
            descItemValuesMap.put(entry.getKey(), entry.getValue().toDescItemValues());
        }
        return descItemValuesMap;
    }
}
