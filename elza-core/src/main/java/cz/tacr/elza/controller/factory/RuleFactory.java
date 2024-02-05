package cz.tacr.elza.controller.factory;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import org.springframework.stereotype.Service;

@Service
public class RuleFactory {

    public static int convertType(final RulItemSpec.Type type) {
        switch (type) {
            case REQUIRED:
                return 3;
            case RECOMMENDED:
                return 2;
            case POSSIBLE:
                return 1;
            case IMPOSSIBLE:
                return 0;
            default:
                throw new IllegalStateException("Type convert not defined: " + type);
        }
    }

    public static int convertType(final RulItemType.Type type) {
        switch (type) {
            case REQUIRED:
                return 3;
            case RECOMMENDED:
                return 2;
            case POSSIBLE:
                return 1;
            case IMPOSSIBLE:
                return 0;
            default:
                throw new IllegalStateException("Type convert not defined: " + type);
        }
    }

}
