package cz.tacr.elza.print.format;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.print.item.ItemSpec;

public enum SpecTitleSource {
	SHORTCUT {
		@Override
		public String getValue(ItemSpec spec) {
			String value = spec.getShortcut();
			if(StringUtils.isEmpty(value)) {
				value = spec.getName();
			}
			return value;
		}
	},
	NAME {
		@Override
		public String getValue(ItemSpec spec) {
			return spec.getName();
		}
	};

	public abstract String getValue(ItemSpec spec);
}
