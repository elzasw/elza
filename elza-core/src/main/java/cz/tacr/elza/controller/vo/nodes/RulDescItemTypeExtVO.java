package cz.tacr.elza.controller.vo.nodes;

import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.RulDescItemTypeVO;
import cz.tacr.elza.domain.RulItemTypeExt;


/**
 * VO rozšířený typu hodnoty atributu
 *
 * @author Martin Šlapa
 * @since 14.1.2016
 */
public class RulDescItemTypeExtVO extends RulDescItemTypeVO {

    /**
     * seznam rozšířených specifikací atributu
     */
    private List<RulDescItemSpecExtVO> descItemSpecs;

    public List<RulDescItemSpecExtVO> getDescItemSpecs() {
        return descItemSpecs;
    }

    public void setDescItemSpecs(final List<RulDescItemSpecExtVO> descItemSpecs) {
        this.descItemSpecs = descItemSpecs;
    }

    public static RulDescItemTypeExtVO newInstance(final RulItemTypeExt itemType) {
    	RulDescItemTypeExtVO result = new RulDescItemTypeExtVO();
    	result.setId(itemType.getItemTypeId());
    	result.setDataTypeId(itemType.getDataTypeId());
    	result.setCode(itemType.getCode());
    	result.setName(itemType.getName());
    	result.setShortcut(itemType.getShortcut());
    	result.setDescription(itemType.getDescription());
    	result.setIsValueUnique(itemType.getIsValueUnique());
    	result.setCanBeOrdered(itemType.getCanBeOrdered());
    	result.setUseSpecification(itemType.getUseSpecification());
    	result.setViewOrder(itemType.getViewOrder());
    	result.setType(itemType.getType());
    	result.setRepeatable(itemType.getRepeatable());
    	if (itemType.getRulItemSpecList() != null) {
    		List<RulDescItemSpecExtVO> descItemSpecs = itemType.getRulItemSpecList().stream().map(i -> RulDescItemSpecExtVO.newInstance(i)).collect(Collectors.toList());
    		result.setDescItemSpecs(descItemSpecs);
    	}
    	if (itemType.getViewDefinition() != null) {
    		result.setViewDefinition(itemType.getViewDefinition());
    	}
    	// TODO set fields if required
    	return result;
    }
}
