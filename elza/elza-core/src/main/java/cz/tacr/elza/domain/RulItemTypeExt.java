package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

/**
 * Rozšíření {@link RulItemType} o specifikace
 *
 */

public class RulItemTypeExt extends RulItemType {

    private List<RulItemSpecExt> rulItemSpecList = new LinkedList<>();

	public RulItemTypeExt(RulItemType src, List<RulItemSpec> specs) {
		super(src);

		// copy specifications
		if (specs != null) {
			for (RulItemSpec specSrc : specs) {
				RulItemSpecExt spec = new RulItemSpecExt(specSrc);
				spec.setType(RulItemSpec.Type.IMPOSSIBLE);
				spec.setRepeatable(true);
				rulItemSpecList.add(spec);
			}

			// seřadit dle viewOrder
			rulItemSpecList.sort((o1, o2) -> {
				if (o1.getViewOrder() == null) {
					if (o2.getViewOrder() == null) {
						// compare using IDs
						return o1.getItemSpecId().compareTo(o2.getItemSpecId());
					} else {
						return -1;
					}
				}
				if (o2.getViewOrder() == null) {
					return 1;
				}
				return o1.getViewOrder().compareTo(o2.getViewOrder());
			});
		}
	}

    /**
     *
     * @return podtypy typů atributů.
     */
    public List<RulItemSpecExt> getRulItemSpecList() {
        return this.rulItemSpecList;
    }

    /**
     *
     * @param rulDescItemSpecList podtypy typů atributů.
     */
    public void setRulItemSpecList(final List<RulItemSpecExt> rulDescItemSpecList) {
        this.rulItemSpecList = rulDescItemSpecList;
    }

    /**
     * Set maximum type according specifications
     */
    public void setTypeMaxFromSpecs() {
        Validate.isTrue(this.getUseSpecification());

        if (CollectionUtils.isEmpty(rulItemSpecList)) {
            setType(Type.IMPOSSIBLE);
        }

        Type maxType = Type.IMPOSSIBLE;
        for (RulItemSpecExt itemSpec : rulItemSpecList) {
            switch (itemSpec.getType()) {
            case IMPOSSIBLE:
                break;
            case POSSIBLE:
                if (maxType == Type.IMPOSSIBLE) {
                    maxType = Type.POSSIBLE;
                }
                break;
            case RECOMMENDED:
                if (maxType == Type.IMPOSSIBLE || maxType == Type.POSSIBLE) {
                    maxType = Type.RECOMMENDED;
                }
                break;
            case REQUIRED:
                if (maxType != Type.REQUIRED) {
                    maxType = Type.REQUIRED;
                }
                break;
            }
        }

        setType(maxType);
    }
}
