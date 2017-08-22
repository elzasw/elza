package cz.tacr.elza.deimport.sections.context;

import java.util.Objects;

import cz.tacr.elza.domain.ArrDescItem;

class DescItemKey {

	private final String typeCode;

	private final String specCode;

	DescItemKey(String typeCode, String specCode) {
		this.typeCode = Objects.requireNonNull(typeCode);
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
			specCode = Objects.requireNonNull(descItem.getItemSpec().getCode());
		}
		return new DescItemKey(typeCode, specCode);
	}
}
