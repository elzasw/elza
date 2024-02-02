package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulArrangementExtension;

/**
 * VO datového typu {@link cz.tacr.elza.domain.RulArrangementExtension}
 *
 */
public class RulArrangementExtensionVO extends BaseCodeVo {

	public static RulArrangementExtensionVO newInstance(final RulArrangementExtension ext) {
		RulArrangementExtensionVO result = new RulArrangementExtensionVO();
    	result.setId(ext.getArrangementExtensionId());
    	result.setName(ext.getName());
    	result.setCode(ext.getCode());
		return result;
	}
}
