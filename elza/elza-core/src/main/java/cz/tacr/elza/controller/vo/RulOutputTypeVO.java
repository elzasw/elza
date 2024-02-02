package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulOutputType;

/**
 * VO pro typ outputu.
 *
 */
public class RulOutputTypeVO extends BaseCodeVo {

	public static RulOutputTypeVO newInstance(final RulOutputType outputType) {
		RulOutputTypeVO result = new RulOutputTypeVO();
		result.setId(outputType.getOutputTypeId());
		result.setName(outputType.getName());
		result.setCode(outputType.getCode());
		return result;
	}
}
