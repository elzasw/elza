package cz.tacr.elza.dataexchange.output.filters.conditions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.dataexchange.output.filters.FilterRuleContext;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

public class PartCondition implements EntityCondition {
	/**
	 * Expected entity class
	 */
	protected String partType;
	
	/**
	 * Expected item type
	 */
	protected String itemType;
	
	/**
	 * Condition is true if the part is younger than specified years
	 */
	protected Integer yuongerThenYears;
	
	public PartCondition() {
		
	}
	
	@Override
	public boolean isTrue(FilterRuleContext frCtx, CachedAccessPoint ap) {
		for(var part: ap.getApParts()) {
			if(Objects.equals(partType, part.getPartType().getCode())) {
				return validateItems(ap, part);
			}
		}

		return false;
	}

	private boolean validateItems(CachedAccessPoint ap, ApPart part) {
		var cachedPart = ap.getPart(part.getPartId());
		if(cachedPart == null) {
			// Unexpected state
			return false;
		}
		if(itemType != null) {
			List<ApItem> items = getItemsByType(cachedPart, itemType);
			if(CollectionUtils.isEmpty(items)) {
				// Item not found
				return false;
			}
			// check item value
			if(this.yuongerThenYears != null) {				
				for(var item: items) {
					var data = item.getData();
					if(data==null) {
						continue;
					}
					data = HibernateUtils.unproxy(data);
					if(data instanceof ArrDataUnitdate) {
						var unitdate = (ArrDataUnitdate) data;
						var from = UnitDateConverter.getLocalDateTimeFromUnitDate(unitdate, true);
						LocalDateTime now = LocalDateTime.now();
						var limit = now.minusYears(this.yuongerThenYears);
						if(from.isAfter(limit)) {
							return true;
						}
					} else {
						// Unsupported data type
						throw new IllegalStateException("Unsupported data type for item " + item.getItemId());
					}
				}
				return false;
			}
		}
		return true;
	}

	private List<ApItem> getItemsByType(CachedPart cachedPart, final String itemType) {
		if(cachedPart.getItems()==null) {
			return List.of();
		}
		return cachedPart.getItems().stream().filter(
				itm -> Objects.equals(itemType, itm.getItemType().getCode() ) 
				)
				.collect(Collectors.toList());
	}

	public String getPartType() {
		return partType;
	}

	public void setPartType(String partType) {
		this.partType = partType;
	}

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public Integer getYuongerThenYears() {
		return yuongerThenYears;
	}

	public void setYuongerThenYears(Integer yuongerThenYears) {
		this.yuongerThenYears = yuongerThenYears;
	}	

}
