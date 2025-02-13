package cz.tacr.elza.domain;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Typy pro generování výchozí hodnoty.
 *
 * Používané pro:
 *  - {@link RptParam}
 */
public enum RptDefaultValueGenerator {
	END_OF_DAY {
		@Override
		public Object getDefaultValue() {
			// if nanoOfSecond == 999_999_999 - in query we get +1 day, so decrease it by 999
			return LocalDate.now().atTime(23, 59, 59, 999_999_000).atOffset(ZoneOffset.UTC);
		}
	},
	START_OF_YEAR {
		@Override
		public Object getDefaultValue() {
			return LocalDate.of(LocalDate.now().getYear(), 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
		}
	};

	abstract public Object getDefaultValue();
}
