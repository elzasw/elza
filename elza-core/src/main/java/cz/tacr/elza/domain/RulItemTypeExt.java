package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


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
				// set this type as parent
				spec.setItemType(this);
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
}
