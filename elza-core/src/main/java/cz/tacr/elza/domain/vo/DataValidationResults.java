package cz.tacr.elza.domain.vo;

import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.vo.DataValidationResult.ValidationResultType;

/**
 * Object for validation results
 * 
 * @author Petr Pytelka
 *
 */
public class DataValidationResults {
	
	/**
	 * List of results
	 */
	List<DataValidationResult> results = new LinkedList<>();
	
	public List<DataValidationResult> getResults() {
		return results;
	}

	/**
	 * Create error description
	 * @param item 		Item with error
	 * @param errorMsg	Error description
	 * @return Object with error description
	 */
    public DataValidationResult createError(final ArrDescItem item, final String errorMsg) {
        DataValidationResult result = new DataValidationResult(ValidationResultType.ERROR);
        result.setDescItem(item);
        result.setMessage(errorMsg);
        
        results.add(result);
        return result;
    }

	/**
	 * Create error description
	 * @param descItemId 	Item with error
	 * @param errorMsg		Error description
	 * @return Object with error description
	 */
    public DataValidationResult createError(final Integer descItemId, final String errorMsg){
        DataValidationResult result = new DataValidationResult(ValidationResultType.ERROR);
        result.setDescItemId(descItemId);
        result.setMessage(errorMsg);
        
        results.add(result);
        return result;
    }

    public DataValidationResult createMissing(final RulDescItemType type,
                                                     final RulDescItemSpec spec) 
    {
        DataValidationResult result = new DataValidationResult(ValidationResultType.MISSING);
        result.setType(type);
        result.setSpec(spec);

        if (spec == null) {
            result.setMessage("Prvek " + type.getName() + " musí být vyplněn.");
        } else {
            result.setMessage("Prvek " + type.getName() + " se specifikací " + spec.getName()
                    + " musí být vyplněn.");
        }
        
        results.add(result);
        return result;
    }

    public DataValidationResult createMissing(final String typeCode, final String message){
        DataValidationResult result = new DataValidationResult(ValidationResultType.MISSING);
        result.setTypeCode(typeCode);
        result.setMessage(message);
        
        results.add(result);

        return result;
    }	
}
