package cz.tacr.elza.drools.model.item;

import java.time.LocalDateTime;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.converter.UnitDateConverter;

public class UnitdateItem extends AbstractItem {
	
	IUnitdate value;

	public UnitdateItem(Integer id, final ItemType itemType, final RulItemSpec itemSpec, IUnitdate value) {
		super(id, itemType, itemSpec);
		this.value = value;
	}
	
	public IUnitdate getValue() {
		return value;
	}
	
	// lower bound of value is not lower than other
	// and upper bound of value is not lower than other
	public boolean startsOrEndsAfter(UnitdateItem otherItem) {
		IUnitdate otherValue = otherItem.getValue();
		int result = UnitDateConverter.compare(value, false, otherValue, false);
		if (result > 0) {
			return true;
		}
		result = UnitDateConverter.compare(value, true, otherValue, true);
		return result > 0;
	}
	
	// lower bound of value is after lower bound of other item
	public boolean startsAfterStart(UnitdateItem otherItem) {
		IUnitdate otherValue = otherItem.getValue();
		int result = UnitDateConverter.compare(value, true, otherValue, true);
		return (result > 0);
	}

	// lower bound of value is after upper bound of other
	public boolean startsAfterEnd(UnitdateItem otherItem) {
		IUnitdate otherValue = otherItem.getValue();
		int result = UnitDateConverter.compare(value, true, otherValue, false);
		return (result > 0);
	}

	// upper bound of value is after lower bound of other item
	public boolean endsAfterStart(UnitdateItem otherItem) {
		IUnitdate otherValue = otherItem.getValue();
		int result = UnitDateConverter.compare(value, false, otherValue, true);
		return (result > 0);
	}

	// upper bound of value is after upper bound of other item
	public boolean endsAfterEnd(UnitdateItem otherItem) {
		IUnitdate otherValue = otherItem.getValue();
		int result = UnitDateConverter.compare(value, false, otherValue, false);
		return (result > 0);
	}

	/**
	 * Compare lower bound of value with other date
	 * @param otherDate
	 * @return
	 */
    public boolean endsBefore(LocalDateTime otherDate) {
    	LocalDateTime lowerBound = UnitDateConverter.getLocalDateTimeFromUnitDate(value, true);

        return lowerBound.isBefore(otherDate);
    }
}
