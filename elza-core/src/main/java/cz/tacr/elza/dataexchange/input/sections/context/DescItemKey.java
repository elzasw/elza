package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.Objects;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrDescItem;

final class DescItemKey {

	private final String typeCode;

	private final String specCode;

	public DescItemKey(String typeCode, String specCode) {
		this.typeCode = Validate.notNull(typeCode);
		this.specCode = specCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null) {
			if (obj == this) {
				return true;
			}
			if (obj.getClass() == DescItemKey.class) {
				DescItemKey o = (DescItemKey) obj;
				return typeCode.equals(o.typeCode) && Objects.equals(specCode, o.specCode);
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeCode, specCode);
	}

	public static DescItemKey of(ArrDescItem descItem) {
		String typeCode = descItem.getItemType().getCode();
		String specCode = null;
		if (descItem.getItemSpec() != null) {
			specCode = Validate.notNull(descItem.getItemSpec().getCode());
		}
		return new DescItemKey(typeCode, specCode);
	}
}
