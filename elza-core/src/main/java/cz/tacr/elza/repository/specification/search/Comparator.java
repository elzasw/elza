package cz.tacr.elza.repository.specification.search;



import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.core.data.DataType;

import jakarta.persistence.criteria.Predicate;

public interface Comparator {

    default String unimplementedMessage(QueryComparator comparator, DataType code) {
        return "Nepodporovaný způsob vyhledání '" + comparator + "' v datovém typu '" + code + "'";
    }

    Predicate toPredicate(QueryComparator comparator, String value);

}
