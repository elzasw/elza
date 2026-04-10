package cz.tacr.elza.common.datetime;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class DateTimeConvertor {
	public static LocalDate toLocalDate(Date dateToConvert) {
		return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
