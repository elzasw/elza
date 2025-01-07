package cz.tacr.elza.validation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverterConsts;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UnitDateValidator implements ConstraintValidator<ValidUnitDate, ArrDataUnitdate> {
	
	private static Logger logger = LoggerFactory.getLogger(UnitDateValidator.class);
	
    @Value("${elza.validate.unitdate.enabled:false}")
    protected boolean enabled = false;
        
    @PostConstruct
	public void init() {
    	logger.debug("Initializing UnitDateValidator (postConstruct), enabled={}", enabled);
	}
	
	@Override
	public void initialize(ValidUnitDate constraintAnnotation) {
		logger.debug("Initializing UnitDateValidator, enabled={}", enabled);
	}

	@Override
	public boolean isValid(ArrDataUnitdate value, ConstraintValidatorContext context) {
		if(value==null) {
			return true;
		}
		
		if(!enabled) {
			return true;
		}
		
		String errorDescription = validate(value);
		if(errorDescription!=null) {
			if(StringUtils.isNotBlank(errorDescription)) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate(errorDescription).addConstraintViolation();
			}
			return false;
		}
		return true;
	}

    /**
     * Full validation
     * @return Error description. Return null if no errors.
     */
	private String validate(IUnitdate value) {
		// check non null values
		String valueFrom = value.getValueFrom();
		if(valueFrom==null) {
			return "Value from is null";
		}
		String valueTo = value.getValueTo();
		if(valueTo==null) {
			return "Value to is null";
		}
		String format = value.getFormat();
		if(format==null) {
			return "Format is null";
		}
		Boolean valueFromEstimated = value.getValueFromEstimated();
		Boolean valueToEstimated = value.getValueToEstimated();
		
		// parse string values from and to
		LocalDateTime from = LocalDateTime.parse(valueFrom, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		int fromYear = from.get(ChronoField.YEAR);
		int fromMonth = from.getMonthValue();
		int fromDay = from.getDayOfMonth();
		int fromHour = from.getHour();
		int fromMinute = from.getMinute();
		int fromSecond = from.getSecond();
		
		LocalDateTime to = LocalDateTime.parse(valueTo, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		int toYear = to.get(ChronoField.YEAR);
		int toMonth = to.getMonthValue();
		int toDay = to.getDayOfMonth();
		int toHour = to.getHour();
		int toMinute = to.getMinute();
		int toSecond = to.getSecond();
		
		// determine single format
		switch (format) {
		case UnitDateConverterConsts.CENTURY:
			if(!validateCenturyFrom(fromYear, fromMonth, fromDay, fromHour, fromMinute, fromSecond)) {
				return "Not a valid century start: " + valueFrom;				
			}
			if(!validateCenturyTo(toYear, toMonth, toDay, toHour, toMinute, toSecond)) {
				return "Not a valid century end: " + valueTo;				
			}
			if((toYear-fromYear)!=99) {
				return "From year to year is not 99 years: " + toYear + "-" + fromYear;
			}
			if(!Objects.equals(valueFromEstimated, valueToEstimated)) {
				return "Inconsistent valueFromEstimated and valueToEstimated.";
			}
			return null;
		case UnitDateConverterConsts.YEAR:
			if(!validateYearFrom(fromMonth, fromDay, fromHour, fromMinute, fromSecond)) {
				return "Not a valid year from: " + valueFrom;				
			}
			if(!validateYearTo(toMonth, toDay, toHour, toMinute, toSecond)) {
				return "Not a valid year end: " + valueTo;				
			}
			if(toYear!=fromYear) {
				return "Not a same year: " + valueFrom + " - " + valueTo;
			}
			if(!Objects.equals(valueFromEstimated, valueToEstimated)) {
				return "Inconsistent valueFromEstimated and valueToEstimated.";
			}
			return null;
		case UnitDateConverterConsts.YEAR_MONTH:
			if(!validateYearMonthFrom(fromDay, fromHour, fromMinute, fromSecond)) {
				return "Not a valid start of month from: " + valueFrom;
			}
			if(!validateYearMonthTo(toYear, toMonth, toDay, toHour, toMinute, toSecond)) {
				return "Not a valid end of month: " + valueTo;
			}
			if(toYear!=fromYear || toMonth!=fromMonth) {
				return "Not a same year/month: " + valueFrom + " - " + valueTo;
			}
			if(!Objects.equals(valueFromEstimated, valueToEstimated)) {
				return "Inconsistent valueFromEstimated and valueToEstimated.";
			}
			return null;
		case UnitDateConverterConsts.DATE:
			if(!validateDateFrom(fromHour, fromMinute, fromSecond)) {
				return "Not a valid start of day: " + valueFrom;
			}
			if(!validateDateTo(toHour, toMinute, toSecond)) {
				return "Not a valid end of day: " + valueTo;
			}
			if(toYear!=fromYear || toMonth!=fromMonth || toDay!=fromDay) {
				return "Not a same date: " + valueFrom + " - " + valueTo;
			}
			if(!Objects.equals(valueFromEstimated, valueToEstimated)) {
				return "Inconsistent valueFromEstimated and valueToEstimated.";
			}
			return null;
		case UnitDateConverterConsts.DATE_TIME:
			if(toYear!=fromYear || toMonth!=fromMonth || toDay!=fromDay ||
					toHour!=fromHour || toMinute!=fromMinute || toSecond!=fromSecond) {
				return "Not a same date and time: " + valueFrom + " - " + valueTo;
			}
			if(!Objects.equals(valueFromEstimated, valueToEstimated)) {
				return "Inconsistent valueFromEstimated and valueToEstimated.";
			}
			return null;
		}
		// format is X-X
		String formatParts[] = StringUtils.split(format, '-');
		if (formatParts.length != 2) {
			return "Format " + format + " is invalid";
		}
		// check format from
		switch (formatParts[0]) {
		case UnitDateConverterConsts.CENTURY:
			if(!validateCenturyFrom(fromYear, fromMonth, fromDay, fromHour, fromMinute, fromSecond)) {
				return "Not a valid century start: " + valueFrom;				
			}
			// check if more then 100 years
			if((toYear-fromYear)<=99) {
				return "From year to year is not more then 100 years: " + toYear + "-" + fromYear; 
			}
			break;
		case UnitDateConverterConsts.YEAR:
			if(!validateYearFrom(fromMonth, fromDay, fromHour, fromMinute, fromSecond)) {
				return "Not a valid year start: " + valueFrom;				
			}
			if(toYear<=fromYear) {
				return "Has to be at least one year later: " + valueFrom + " - " + valueTo;
			}
			break;
		case UnitDateConverterConsts.YEAR_MONTH:
			if(!validateYearMonthFrom(fromDay, fromHour, fromMinute, fromSecond)) {
				return "Not a valid start of month from: " + valueFrom;
			}
			if(toYear<fromYear || (toYear==fromYear && toMonth<=fromMonth)) {
				return "Has to be at least one month later: " + valueFrom + " - " + valueTo;
			}
			break;
		case UnitDateConverterConsts.DATE:
			if(!validateDateFrom(fromHour, fromMinute, fromSecond)) {
				return "Not a valid start of day: " + valueFrom;
			}
			if(toYear<fromYear || (toYear==fromYear && 
					(toMonth<fromMonth || (toMonth==fromMonth && toDay<=fromDay)))) {
				return "Has to be at least one day later: " + valueFrom + " - " + valueTo;
			}
			break;			
		case UnitDateConverterConsts.DATE_TIME:
			if(toYear<fromYear || (toYear==fromYear && 
					(toMonth<fromMonth || (toMonth==fromMonth && 
					(toDay<fromDay || (toDay==fromDay && 
					(toHour<fromHour || (toHour==fromHour && 
					(toMinute<fromMinute || (toMinute==fromMinute && toSecond<=fromSecond)))))))))) {
				return "Has to be at least one second later: " + valueFrom + " - " + valueTo;
			}
			break;
		default:
			return "Format " + format + " is invalid";
		}
		// check format to
		switch (formatParts[1]) {
		case UnitDateConverterConsts.CENTURY:
			if(!validateCenturyTo(toYear, toMonth, toDay, toHour, toMinute, toSecond)) {
				return "Not a valid century end: " + valueTo;				
			}
			// check if more then 100 years
			if((toYear-fromYear)<=99) {
				return "From year to year is not more then 100 years: " + toYear + "-" + fromYear; 
			}
			break;
		case UnitDateConverterConsts.YEAR:
			if(!validateYearTo(toMonth, toDay, toHour, toMinute, toSecond)) {
				return "Not a valid year end: " + valueTo;				
			}
			if(toYear<=fromYear) {
				return "Has to be at least one year later: " + valueFrom + " - " + valueTo;
			}
			break;
		case UnitDateConverterConsts.YEAR_MONTH:
			if(!validateYearMonthTo(toYear, toMonth, toDay, toHour, toMinute, toSecond)) {
				return "Not a valid end	of month: " + valueTo;
			}
			if(toYear<fromYear || (toYear==fromYear && toMonth<=fromMonth)) {
				return "Has to be at least one month later: " + valueFrom + " - " + valueTo;
			}
			break;
		case UnitDateConverterConsts.DATE:
			if(!validateDateTo(toHour, toMinute, toSecond)) {
				return "Not a valid end of day: " + valueTo;
			}
			if (toYear < fromYear || (toYear == fromYear && 
					(toMonth < fromMonth || (toMonth == fromMonth && toDay <= fromDay)))) {
				return "Has to be at least one day later: " + valueFrom + " - " + valueTo;
			}
			break;			
		case UnitDateConverterConsts.DATE_TIME:
			if (toYear < fromYear || (toYear == fromYear && (toMonth < fromMonth || (toMonth == fromMonth
					&& (toDay < fromDay || (toDay == fromDay && (toHour < fromHour || (toHour == fromHour
							&& (toMinute < fromMinute || (toMinute == fromMinute && toSecond <= fromSecond)))))))))) {
				return "Has to be at least one second later: " + valueFrom + " - " + valueTo;
			}
			break;
		default:
			return "Format " + format + " is invalid";
		}
		
		return null;
	}

	private boolean validateDateTo(int toHour, int toMinute, int toSecond) {
		return (toHour==23) && (toMinute==59) && (toSecond==59);
	}

	private boolean validateDateFrom(int fromHour, int fromMinute, int fromSecond) {
		return (fromHour==0) && (fromMinute==0) && (fromSecond==0);
	}

	private boolean validateYearMonthTo(int toYear, int toMonth, int toDay, int toHour, int toMinute, int toSecond) {
		// get last day of month
		// e.g. ends at 1843-12-31
		
		// it is better to use Calendar API due to support of years BC
		// instead of using java.time.YearMonth
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, toYear);
		cal.set(Calendar.MONTH, toMonth-1);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		return (toDay==lastDay) && 
				(toHour==23) && (toMinute==59) && (toSecond==59);
	}

	private boolean validateYearMonthFrom(int fromDay, int fromHour, int fromMinute, int fromSecond) {
		// e.g. starts at 1843-1-1
		return (fromDay==1) && 
				(fromHour==0) && (fromMinute==0) && (fromSecond==0);
	}

	private boolean validateYearFrom(int fromMonth, int fromDay, int fromHour, int fromMinute,
			int fromSecond) {
		// e.g. starts at 1843-1-1
		return (fromMonth==1) && 
				(fromDay==1) && 
				(fromHour==0) && (fromMinute==0) && (fromSecond==0);
	}

	private boolean validateYearTo(int toMonth, int toDay, int toHour, int toMinute, int toSecond) {
		// e.g. ends at 1843-12-31
		return (toMonth==12) && 
				(toDay==31) && 
				(toHour==23) && (toMinute==59) && (toSecond==59);
	}

	private boolean validateCenturyTo(int toYear, int toMonth, int toDay, int toHour, int toMinute, int toSecond) {
		// e.g. 2nd century ends at 31.12.100
		return (toYear % 100 == 0) && 
				(toMonth==12) && 
				(toDay==31) && 
				(toHour==23) && (toMinute==59) && (toSecond==59);
	}

	private boolean validateCenturyFrom(int fromYear, int fromMonth, int fromDay, int fromHour, int fromMinute,
			int fromSecond) {
		// e.g. 2nd century starts at 1.1.101
		return ((fromYear-1) % 100 == 0) && 
				(fromMonth==1) && 
				(fromDay==1) && 
				(fromHour==0) && (fromMinute==0) && (fromSecond==0);
	}

}
