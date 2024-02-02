package cz.tacr.elza.controller.vo.nodes;

import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.domain.RulItemSpec;


/**
 * VO rozšířené specifikace
 *
 * @since 14.1.2016
 */
public class RulDescItemSpecExtVO extends RulDescItemSpecVO {

	public static RulDescItemSpecExtVO newInstance(final RulItemSpec itemSpec) {
		RulDescItemSpecExtVO result = new RulDescItemSpecExtVO();
    	result.setId(itemSpec.getItemSpecId());
    	result.setName(itemSpec.getName());
    	result.setCode(itemSpec.getCode());
    	result.setShortcut(itemSpec.getShortcut());
    	result.setDescription(itemSpec.getDescription());
    	result.setViewOrder(itemSpec.getViewOrder());
    	result.setType(itemSpec.getType());
    	result.setRepeatable(itemSpec.getRepeatable());
    	return result;
	}
}
