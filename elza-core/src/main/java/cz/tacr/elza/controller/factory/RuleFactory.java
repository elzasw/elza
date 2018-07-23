package cz.tacr.elza.controller.factory;

import cz.tacr.elza.controller.vo.nodes.DescItemSpecLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleFactory {

    public ItemTypeLiteVO createVO(final RulItemTypeExt itemType) {
        ItemTypeLiteVO vo = new ItemTypeLiteVO();
        List<RulItemSpecExt> rulItemSpecs = itemType.getRulItemSpecList();
        List<DescItemSpecLiteVO> specItems = new ArrayList<>();
        for (RulItemSpecExt rulItemSpec : rulItemSpecs) {
            if (rulItemSpec.getType() != RulItemSpec.Type.IMPOSSIBLE) {
                specItems.add(createVO(rulItemSpec));
            }
        }

        vo.setId(itemType.getItemTypeId());
        vo.setType(convertType(itemType.getType()));
        vo.setRep(itemType.getRepeatable() ? 1 : 0);
        vo.setSpecs(specItems);
        vo.setWidth(1); // není zatím nikde definované
        vo.setCalSt(itemType.getCalculableState() ? 1 : 0);
        vo.setCal(itemType.getCalculable() ? 1 : 0);
        vo.setInd(itemType.getIndefinable() ? 1 : 0);
        vo.setFavoriteSpecIds(null); // TODO

        return vo;
    }

    public DescItemSpecLiteVO createVO(final RulItemSpecExt itemSpec) {
        DescItemSpecLiteVO vo = new DescItemSpecLiteVO();

        vo.setId(itemSpec.getItemSpecId());
        vo.setType(convertType(itemSpec.getType()));
        vo.setRep(itemSpec.getRepeatable() ? 1 : 0);

        return vo;
    }

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
