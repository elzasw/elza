package cz.tacr.elza.domain.converter;

import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.CENTURY;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DATE;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DATE_TIME;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DEFAULT_INTERVAL_DELIMITER;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.ESTIMATED_TEMPLATE;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.FORMAT_DELIMITER;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.YEAR;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.YEAR_MONTH;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Konvertor pro správné zobrazování UnitDate podle formátu.
 *
 * @since 6.11.2015
 */
public class UnitDateConverter {
	
	private static final String BC_POSTFIX = " př. n. l.";
	
    /**
     * Šablona pro století
     */
    public static final String CENTURY_TEMPLATE = "%d. st.";
    
    /**
     * Century BC
     */
    public static final String CENTURY_BC_TEMPLATE = "%d. st. př. n. l.";
        
    /**
     * Template for year BC
     */
    public static final String YEAR_BC_TEMPLATE = "%d př. n. l.";

    /**
     * Formát datumu
     */
    public static final String FORMAT_DATE = "d.M.u";

    /**
     * Formát datumu s časem
     */
    public static final String FORMAT_DATE_TIME = "d.M.u H:mm:ss";

    /**
     * Formát datumu s časem
     */
    public static final String FORMAT_DATE_TIME_WITHOUT_SEC = "d.M.u H:mm";

    /**
     * Formát roku s měsícem
     */
    public static final String FORMAT_YEAR_MONTH = "M.u";

    /**
     * Šablona pro interval
     */
    public static final String DEFAULT_INTERVAL_DELIMITER_TEMPLATE = "%s-%s";

    /**
     * Oddělovač pro interval, který vyjadřuje odhad
     */
    public static final String ESTIMATE_INTERVAL_DELIMITER = "/";

    /**
     * Šablona pro interval, který vyjadřuje odhad
     */
    public static final String ESTIMATE_INTERVAL_DELIMITER_TEMPLATE = "%s/%s";

    /**
     * Když druhý rok v intervalu je negativní
     */
    public static final String SECOND_YEAR_IS_NEGATIVE = "--";

    /**
     * Suffix př. n. l.
     */
    public static final String PR_N_L = " př. n. l.";

    /**
     * Záporná reprezentace v ISO formátu.
     */
    public static final String BC_ISO = "-";

    private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern(FORMAT_DATE);
    private static final DateTimeFormatter FORMATTER_DATE_TIME = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME);
    private static final DateTimeFormatter FORMATTER_DATE_TIME2 = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME_WITHOUT_SEC);
    private static final DateTimeFormatter FORMATTER_YEAR_MONTH = DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH);
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    enum TokenType {
    	SINGLE,
    	FROM,
    	TO
    }

    /**
     * Parsed date part
     */
    private static class Token {
    	
    	private String format;
    	
    	private boolean estimate = false;

		public Token(final String format, final boolean estimate) {
    		this.format = format;
    		this.estimate = estimate;
    	}
		
		public String getFormat() {
			return format;
		}

        public LocalDateTime dateFrom = null;

        public LocalDateTime dateTo = null;        
    }
    
    /**
     * Parser for date part
     */
    interface DatePartParser {
    	
    	/**
    	 * Parse string to token
    	 * @param tokenString
    	 * @return Return null if string was not parsed.
    	 * 		Else return token.
    	 * @throws Throws RuntimeException if parser failed.
    	 */
		Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type);
	}
    
    static class CenturyParser implements DatePartParser {
        /**
         * Výraz pro detekci století
         */
        public static final String EXP_CENTURY = "(\\d+)((st)|(\\.[ ]?st\\.))";
        public static Pattern patternCentury = Pattern.compile(EXP_CENTURY);

		@Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			Matcher matcher = patternCentury.matcher(tokenString);
			if(!matcher.matches()) {
				return null;
			}
            Integer c = Integer.parseInt(matcher.group(1));
	        if(negative) {
				c = -c+1;
			}
	        return createCentury(c, estimate);
			
		}
    	
    }
    
    static class CenturyParserBC implements DatePartParser {

        /**
         * Výraz pro detekci století př.n.l.
         */
    	public static final String EXP_CENTURY_BC = "(\\d+)(\\.[ ]?st\\.[ ]?př\\.[ ]?n\\.[ ]?l\\.)";
    	public static Pattern patternCenturyBC = Pattern.compile(EXP_CENTURY_BC);

    	@Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			Matcher matcher = patternCenturyBC.matcher(tokenString);
			if(!matcher.matches()) {
				return null;
			}
            if(negative) {
            	throw new SystemException("Double negative not supported", BaseCode.PROPERTY_IS_INVALID);
            }
            Integer c = -Integer.parseInt(matcher.group(1))+1;
            return createCentury(c, estimate);
		}    	
    }
    
    static class YearParser implements DatePartParser {
		/**
		 * Výraz pro detekci roku
		 */
		public static final String EXP_YEAR = "(-?\\d{1,4})";
		public static Pattern patternYear = Pattern.compile(EXP_YEAR);

		@Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			Matcher matcher = patternYear.matcher(tokenString);
			if(!matcher.matches()) {
				return null;
			}
			Integer year = Integer.parseInt(matcher.group(1));
			if(negative) {
				year = -year+1;
			}
			
	        Token token = new Token(YEAR, estimate);
	        token.dateFrom = LocalDateTime.of(year, 1, 1, 0, 0);
	        token.dateTo = LocalDateTime.of(year, 12, 31, 23, 59, 59);
	        return token;
		}
    }
    
    static class YearParserBC implements DatePartParser {
        /**
         * Year BC
         */
        public static final String EXP_YEAR_BC = "(\\d{1,5})([ ]?př\\.[ ]?n\\.[ ]?l\\.)";
        public static Pattern patternYearBC = Pattern.compile(EXP_YEAR_BC);

		@Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			Matcher matcher = patternYearBC.matcher(tokenString);
			if(!matcher.matches()) {
				return null;
			}
			if(negative) {
				throw new SystemException("Double negative not supported", BaseCode.PROPERTY_IS_INVALID);
			}
			Integer year = -Integer.parseInt(matcher.group(1))+1;
			
			Token token = new Token(YEAR, estimate);
	        token.dateFrom = LocalDateTime.of(year, 1, 1, 0, 0);
	        token.dateTo = LocalDateTime.of(year, 12, 31, 23, 59, 59);
	        return token;
		}
    }
    
    static class YearMonthParser implements DatePartParser {

		@Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			YearMonth yearMonth;
	        try {
	            yearMonth = YearMonth.parse(tokenString, FORMATTER_YEAR_MONTH);	    		
	    	} catch (DateTimeParseException e) {
		    	return null;
	    	}	        
	        
	        int year = yearMonth.getYear();
			if(negative) {
				year = (-year+1);
			}
			LocalDate date = LocalDate.of(year, yearMonth.getMonth(), 1);	            
	        LocalDateTime dateTime = LocalDateTime.from(date.atStartOfDay());
	            
	        Token token = new Token(YEAR_MONTH, estimate);
	        token.dateFrom = dateTime;
	        dateTime = dateTime.plusMonths(1);
	        token.dateTo = dateTime.minusSeconds(1);

	        return token;
		}
    }
    
    static class YearMonthParserBC implements DatePartParser {

        public static final String EXP_YEAR_MONTH_BC = "(\\d{1,2})(\\.)(\\d{1,5})([ ]?př\\.[ ]?n\\.[ ]?l\\.)";
        public static Pattern patternYearMonthBC = Pattern.compile(EXP_YEAR_MONTH_BC);

        @Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			Matcher matcher = patternYearMonthBC.matcher(tokenString);
			if(!matcher.matches()) {
				return null;
			}
			if(negative) {
				throw new SystemException("Double negative not supported", BaseCode.PROPERTY_IS_INVALID);
			}
			Integer month = Integer.parseInt(matcher.group(1));
			Integer year = -Integer.parseInt(matcher.group(3))+1;			
	        
			LocalDate date = LocalDate.of(year, month, 1);	            
	        LocalDateTime dateTime = LocalDateTime.from(date.atStartOfDay());
	            
	        Token token = new Token(YEAR_MONTH, estimate);
	        token.dateFrom = dateTime;
	        dateTime = dateTime.plusMonths(1);
	        token.dateTo = dateTime.minusSeconds(1);

	        return token;
		}
    }

    static class DateTimeParser implements DatePartParser {

		@Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
	        LocalDateTime date;
	        int offsetSeconds = 0;
	        try {
	            date = LocalDateTime.parse(tokenString, FORMATTER_DATE_TIME);
	        } catch (DateTimeParseException e) {
	        	try {
	        		date = LocalDateTime.parse(tokenString, FORMATTER_DATE_TIME2);
	        		offsetSeconds = 59;
	        	} catch (DateTimeParseException e2) {
		        	return null;
	        	}
	        }
	        
	        String format = DATE_TIME;
	        if((offsetSeconds>0) && type==TokenType.SINGLE) {
	        	format = DATE_TIME+FORMAT_DELIMITER+DATE_TIME;
	        }
	        Token token = new Token(format, estimate);
            token.dateFrom = date;
            // Should we create time interval for second format without seconds?
            token.dateTo = (offsetSeconds>0) ? date.plusSeconds(offsetSeconds) : date;
            
	        if(negative) {
	        	token.dateFrom = token.dateFrom.minusYears(2*token.dateFrom.getYear()-1);
	        	token.dateTo = token.dateTo.minusYears(2*token.dateFrom.getYear()-1);
	        }
	        return token;
		}
    }
    
    static class DateTimeParserBC implements DatePartParser {

        public static final String EXP_DATE_TIME_BC = "(\\d{1,2})(\\.)(\\d{1,2})(\\.)(\\d{1,5})([ ])(\\d{1,2})(:)(\\d{1,2})(:)(\\d{1,2})([ ]?př\\.[ ]?n\\.[ ]?l\\.)";
        public static Pattern patternDateTimeBC = Pattern.compile(EXP_DATE_TIME_BC);

        // without seconds
        public static final String EXP_DATE_TIME_BC2 = "(\\d{1,2})(\\.)(\\d{1,2})(\\.)(\\d{1,5})([ ])(\\d{1,2})(:)(\\d{1,2})([ ]?př\\.[ ]?n\\.[ ]?l\\.)";
        public static Pattern patternDateTimeBC2 = Pattern.compile(EXP_DATE_TIME_BC2);

        @Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			Matcher matcher = patternDateTimeBC.matcher(tokenString);	        
	        int offsetSeconds = 0;
			if(!matcher.matches()) {
				matcher = patternDateTimeBC2.matcher(tokenString);
				if(!matcher.matches()) {
					return null;
				}
				offsetSeconds = 59;
			}
			if(negative) {
				throw new SystemException("Double negative not supported", BaseCode.PROPERTY_IS_INVALID);
			}
			Integer day = Integer.parseInt(matcher.group(1));
			Integer month = Integer.parseInt(matcher.group(3));
			Integer year = -Integer.parseInt(matcher.group(5))+1;
			Integer hour = Integer.parseInt(matcher.group(7));
			Integer minute = Integer.parseInt(matcher.group(9));
			Integer second = 0;
			if(offsetSeconds==0) {
				second = Integer.parseInt(matcher.group(11));
			}
	        String format = DATE_TIME;
	        if((offsetSeconds>0) && type==TokenType.SINGLE) {
	        	format = DATE_TIME+FORMAT_DELIMITER+DATE_TIME;
	        }

			LocalDate date = LocalDate.of(year, month, day);
	        LocalDateTime dateTime = LocalDateTime.from(date.atTime(hour, minute, second));
	            
	        Token token = new Token(format, estimate);
	        token.dateFrom = dateTime;
            // Should we create time interval for second format without seconds?
            token.dateTo = (offsetSeconds>0) ? dateTime.plusSeconds(offsetSeconds) : dateTime;
	        return token;
		}
    }

    static class DateParser implements DatePartParser {

		@Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			LocalDate date;
	        try {
	        	date = LocalDate.parse(tokenString, FORMATTER_DATE);
	        } catch (DateTimeParseException e) {
	        	return null;
	        }
            if(negative) {
	            int year = date.getYear();
	            if(year<0) {
		            throw new DateTimeParseException("Invalid date (double negative)", tokenString, 0);
	            }
	            date = LocalDate.of(-year+1, date.getMonth(), date.getDayOfMonth());
            }
	        Token token = new Token(DATE, estimate);
	        LocalDateTime dateTime = LocalDateTime.from(date.atStartOfDay());
	        token.dateFrom = dateTime;
	        dateTime = dateTime.plusDays(1);
	        dateTime = dateTime.minusSeconds(1);
	        token.dateTo = dateTime;
	        return token;
		}
    }

    static class DateParserBC implements DatePartParser {

        public static final String EXP_DATE_BC = "(\\d{1,2})(\\.)(\\d{1,2})(\\.)(\\d{1,5})([ ]?př\\.[ ]?n\\.[ ]?l\\.)";
        public static Pattern patternDateBC = Pattern.compile(EXP_DATE_BC);

        @Override
		public Token parseToken(boolean estimate, boolean negative, String tokenString, TokenType type) {
			Matcher matcher = patternDateBC.matcher(tokenString);
			if(!matcher.matches()) {
				return null;
			}
			if(negative) {
				throw new SystemException("Double negative not supported", BaseCode.PROPERTY_IS_INVALID);
			}
			Integer day = Integer.parseInt(matcher.group(1));
			Integer month = Integer.parseInt(matcher.group(3));
			Integer year = -Integer.parseInt(matcher.group(5))+1;			
	        
			LocalDate date = LocalDate.of(year, month, day);
	        LocalDateTime dateTime = LocalDateTime.from(date.atStartOfDay());
	            
	        Token token = new Token(DATE, estimate);
	        token.dateFrom = dateTime;
	        dateTime = dateTime.plusDays(1);
	        token.dateTo = dateTime.minusSeconds(1);

	        return token;
        }
    }
    
    static List<DatePartParser> parsers = List.of(
    		new YearParser(),
    		new YearMonthParser(),
    		new DateTimeParser(),
    		new DateParser(),
    		new CenturyParser(),
    		new CenturyParserBC(),
    		new YearParserBC(),
    		new YearMonthParserBC(),
    		new DateParserBC(),
    		new DateTimeParserBC());


    /**
     * Provede konverzi textového vstupu a doplní intervaly do objektu.
     *
     * @param input    textový vstup
     * @param unitdate doplňovaný objekt
     * @return doplněný objekt
     */
    public static <T extends IUnitdate> T convertToUnitDate(final String input, final T unitdate) {

        unitdate.setFormat("");

        String normalizedInput = normalizeInput(input);

        try {
            if (isInterval(normalizedInput)) {
                parseInterval(normalizedInput, unitdate);

                LocalDateTime from = null;
                if (unitdate.getValueFrom() != null) {
                    from = LocalDateTime.parse(unitdate.getValueFrom(), FORMATTER_ISO);
                }

                LocalDateTime to = null;
                if (unitdate.getValueTo() != null) {
                    to = LocalDateTime.parse(unitdate.getValueTo(), FORMATTER_ISO);
                }

                if (from != null && to != null && from.isAfter(to)) {
                    throw new IllegalArgumentException("Neplatný interval ISO datumů: od > do");
                }

            } else {
                Token token = parseToken(normalizedInput, TokenType.SINGLE);
                unitdate.formatAppend(token.getFormat());
                unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
                unitdate.setValueFromEstimated(token.estimate);
                unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
                unitdate.setValueToEstimated(token.estimate);
            }

            if (unitdate.getValueFrom() != null) {
                String valueFrom = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                        LocalDateTime.parse(unitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                if (valueFrom.length() != 19 && valueFrom.length() != 20) {
                    throw new IllegalArgumentException("Neplatná délka ISO datumů");
                }
            }

            if (unitdate.getValueTo() != null) {
                String valueTo = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        .format(LocalDateTime.parse(unitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                if (valueTo.length() != 19 && valueTo.length() != 20) {
                    throw new IllegalArgumentException("Neplatná délka ISO datumů");
                }
            }

            normalize(unitdate);

        } catch (Exception e) {
            unitdate.setFormat("");
            throw new SystemException("Vstupní řetězec není validní", e, BaseCode.PROPERTY_IS_INVALID)
                    .set("property", "format")
                    .set("value", input);
        }

        return unitdate;
    }

    /**
     * Vyplnění polí normalizeFrom a normalizeTo
     * 
     * @param aeDataUnitdate
     */
    public static void normalize(IUnitdate aeDataUnitdate) {

        String valueFrom = aeDataUnitdate.getValueFrom();
        if (valueFrom == null) {
            aeDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
        } else {
            LocalDateTime fromDate = LocalDateTime.parse(valueFrom.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            aeDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(fromDate));
        }

        String valueTo = aeDataUnitdate.getValueTo();
        if (valueTo == null) {
            aeDataUnitdate.setNormalizedTo(Long.MAX_VALUE);
        } else {
            LocalDateTime toDate = LocalDateTime.parse(valueTo.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            aeDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(toDate));
        }
    }

    /**
     * Normalizace závorek (na hranaté) a odstranění bílých, přebytečných znaků.
     *
     * @param input text k normalizaci
     * @return normalizovaný text
     */
    private static String normalizeInput(final String input) {
        return input.replace("(", "[").replace(")", "]").trim();
    }

    /**
     * Detekce, zda-li se jedná o interval
     * Interval existuje, pokud je nalezen oddělovač '/' nebo '-', ale vylučujeme situace:
     * Intervaly:
     *      1900-1912
     *      1900/1912
     *      -7-2
     *      -7/-2
     *      -7--2
     *      -1.3.7--14.10.2
     *      [-10--8]
     * Samostatne:
     *      -12.3.44
     *      -18
     *      [-20]
     *
     * @param input vstupní řetězec
     * @return true - jedná se o interval
     */
    private static boolean isInterval(final String input) {
        if (input.contains(ESTIMATE_INTERVAL_DELIMITER)) {
            return true; // 1900/1902
        }
        String dateString = input;
        if (input.startsWith("-")) {
            dateString = dateString.substring(1); // vyloučit -8
        } else if (input.startsWith("[-")) {
            dateString = dateString.substring(2); // vyloučit [-8]
        }

        return dateString.contains(DEFAULT_INTERVAL_DELIMITER);
    }

    /**
     * Parsování intervalu.
     *
     * @param input    textový vstup
     * @param unitdate doplňovaný objekt
     */
    private static void parseInterval(final String input, final IUnitdate unitdate) {
        Token token;
        String[] data = splitInterval(input);

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + input);
        }

        boolean estimateBoth = input.contains(ESTIMATE_INTERVAL_DELIMITER);

        String from = data[0];
		String to = data[1];
		
        token = parseToken(from, TokenType.FROM);
        unitdate.formatAppend(token.getFormat());
        unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
        unitdate.setValueFromEstimated(token.estimate || estimateBoth);
        
        unitdate.formatAppend(DEFAULT_INTERVAL_DELIMITER);
        
        token = parseToken(to, TokenType.TO);
        unitdate.formatAppend(token.getFormat());
        unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
        unitdate.setValueToEstimated(token.estimate || estimateBoth);
    }

    /**
     * Rozdělení řetězce s datovým intervalem na dva řádky
     * 
     * @param input
     * @return
     */
    private static String[] splitInterval(final String input) {
        String delimiter = SECOND_YEAR_IS_NEGATIVE;

        // vzorek: datum/datum
        if (input.contains(ESTIMATE_INTERVAL_DELIMITER)) {
            return input.split(ESTIMATE_INTERVAL_DELIMITER);
        }
        // vzorek: datum-datum
        if (!input.contains(SECOND_YEAR_IS_NEGATIVE)) {
            if (!input.startsWith("-")) {
                return input.split(DEFAULT_INTERVAL_DELIMITER);
            }
            // vzorek: -datum-datum
            delimiter = DEFAULT_INTERVAL_DELIMITER;
        }

        // vzorek: [-]datum--datum
        int position = input.indexOf(delimiter, 1);
        String[] parts = {input.substring(0, position), input.substring(position + 1)};

        return parts;
    }

    /**
     * Provede konverzi formátu do textové podoby.
     * 
     * @param unitdate
     * @return String
     */
    public static String convertToString(final IUnitdate unitdate) {

        String format = unitdate.getFormat();

        if (isInterval(format)) {
            return convertInterval(format, unitdate);
        }
        return convertToken(format, unitdate.getValueFrom(), unitdate.getValueFromEstimated());
    }

    /**
	 * Begin of interval to string
	 *
	 * @param unitdate
	 * @param allowEstimate
     * @return String
	 */
	public static String beginToString(final IUnitdate unitdate, final boolean allowEstimate) {

	    String format = unitdate.getFormat();

	    if (isInterval(format)) {
			String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);
			format = data[0];
		}
	    return convertToken(format, unitdate.getValueFrom(), allowEstimate && unitdate.getValueFromEstimated());
    }

	/**
	 * End of interval to string
	 *
	 * @param unitdate
	 * @return String
	 */
	public static String endToString(final IUnitdate unitdate, final boolean allowEstimate) {

	    String format = unitdate.getFormat();

		if (isInterval(format)) {
			String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);
			format = data[1];
		}
		return convertToken(format, unitdate.getValueTo(), allowEstimate && unitdate.getValueToEstimated());
	}

	/**
	 * Konverze intervalu.
	 *
	 * @param format   vstupní formát
	 * @param unitdate doplňovaný objekt
	 * @return výsledný řetězec
	 */
    private static String convertInterval(final String format, final IUnitdate unitdate) {

        String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + format);
        }

        boolean bothEstimate = BooleanUtils.isTrue(unitdate.getValueFromEstimated()) && BooleanUtils.isTrue(unitdate.getValueToEstimated());

        String template = bothEstimate? ESTIMATE_INTERVAL_DELIMITER_TEMPLATE : DEFAULT_INTERVAL_DELIMITER_TEMPLATE;  
        String dateFrom = convertToken(data[0], unitdate.getValueFrom(), !bothEstimate && unitdate.getValueFromEstimated());
        String dateTo = convertToken(data[1], unitdate.getValueTo(), !bothEstimate && unitdate.getValueToEstimated());

        return String.format(template, dateFrom, dateTo);
    }

    /**
     * Konverze tokenu - výrazu.
     *
     * @param format        vstupní formát
     * @param srcValue		zdrojový řetězec      
     * @param first         zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertToken(final String format, final String srcValue, final boolean estimated) {
    	
    	// remove extra whitespaces
    	String value = srcValue.trim();

        String result;
        boolean addEstimate = estimated;

        LocalDateTime date;
        try {
            date = LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Chyba při analýze datum: " + value, e);
        }

        int year = date.getYear();
        int month = date.getMonthValue();
        // check if date is BC
        // values has to be subsrtacted (-1year]
        boolean bc;
        if (year > 0) {
        	bc = false;
        } else {
        	bc = (year<=0);
        	// transform ISO year to chronological year
        	// 0 -> -1 -> 1 pr. n. l.
        	// -99 -> -100 -> 100 pr. n. l.
        	year = -(year-1);
        	date = LocalDateTime.of(LocalDate.of(year, month, date.getDayOfMonth()), date.toLocalTime());
        }
        
        switch (format) {
            case CENTURY:
            	int century = (year+99) / 100;
            	if(bc) {
            		result = String.format(CENTURY_BC_TEMPLATE, century);
            	} else {
            		result = String.format(CENTURY_TEMPLATE, century);
            	}
                break;
            case YEAR:
            	if(bc) {
            		result = String.format(YEAR_BC_TEMPLATE, year);
            	} else {
            		result = String.valueOf(year);
            	}
                break;
            case YEAR_MONTH:
                result = FORMATTER_YEAR_MONTH.format(date);
                if(bc) {
                	result += BC_POSTFIX;
                }
                break;
            case DATE:
                result = FORMATTER_DATE.format(date);
                if(bc) {
                	result += BC_POSTFIX;
                }
                break;
            case DATE_TIME:
                result = FORMATTER_DATE_TIME.format(date);
                if(bc) {
                	result += BC_POSTFIX;
                }
                break;
            default:
                throw new IllegalStateException("Neexistující formát: " + format);
        }

        if (addEstimate) {
            result = String.format(ESTIMATED_TEMPLATE, result);
        }

        return result;
    }

    /**
     * Konverze roku.
     *
     * @param unitdate doplňovaný objekt
     * @param first zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    public static String convertYear(final IUnitdate unitdate, final boolean first) {
        LocalDateTime date = getLocalDateTimeFromUnitDate(unitdate, first);
        if (date != null) {
        	if(date.getYear()<=0) {
        		return Math.abs(date.getYear()-1) + PR_N_L;
        	} else {
        		return ""+date.getYear();
        	}
        }
        return unitdate.getFormat();
    }

    /**
     * Získání LocalDateTime z objektu IUnitdate.
     * 
     * @param unitdate
     * @param first
     * @return LocalDateTime
     */
    public static LocalDateTime getLocalDateTimeFromUnitDate(final IUnitdate unitdate, final boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                return LocalDateTime.parse(unitdate.getValueFrom());
            }
        } else {
            if (unitdate.getValueTo() != null) {
                return LocalDateTime.parse(unitdate.getValueTo());
            }
        }
        return null;
    }

    /**
     * Parsování tokenu.
     * @param isNegative 
     *
     * @param tokenString výraz
     * @param unitdate    doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseToken(String tokenString, TokenType type) {
        if (StringUtils.isEmpty(tokenString)) {
            throw new IllegalArgumentException("Nemůže existovat prázdný interval");
        }

        boolean estimate = false;
        if (tokenString.charAt(0) == '[' && tokenString.charAt(tokenString.length() - 1) == ']') {
            tokenString = tokenString.substring(1, tokenString.length() - 1);
            estimate = true;
        }
        boolean negative = false;
        if(tokenString.startsWith("-")) {
        	tokenString = tokenString.substring(1);
        	negative = true;
        }

        // try to parse date using available parsers
        for(DatePartParser parser: parsers) {
        	Token result = parser.parseToken(estimate, negative, tokenString, type);
        	if(result!=null) {
	        	return result;
        	}
        }
        throw new IllegalArgumentException("Nepodporovaný výraz: " + tokenString);
    }
    
    private static Token createCentury(Integer c, boolean estimate) {        
        Token token = new Token(CENTURY, estimate);
        try {

            token.dateFrom = LocalDateTime.of((c - 1) * 100 + 1, 1, 1, 0, 0);
            token.dateTo = LocalDateTime.of(c * 100, 12, 31, 23, 59, 59);

        } catch (DateTimeParseException e) {
            throw new SystemException("Failed to create date, century: "+c, BaseCode.PROPERTY_IS_INVALID)
                    .set("value", c);
        }

        return token;
	}

	/**
     * Testování, zda-li odpovídá řetězec formátu
     *
     * @param formatter formát
     * @param s         řetězec
     * @return true - lze parsovat
     */
	private static boolean tryParseDate(final DateTimeFormatter formatter, final String s) {
        try {
            formatter.withResolverStyle(ResolverStyle.STRICT);
            formatter.parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static <T extends IUnitdate> T convertIsoToUnitDate(final String input, final T unitDate) {
        if (tryParseDate(FORMATTER_ISO, input)) {
            unitDate.setValueFrom(input);
            unitDate.setValueFromEstimated(false);
            unitDate.setValueTo(input);
            unitDate.setValueToEstimated(true);
        } else {
            int isoLength = 19;
            if (input.startsWith(BC_ISO)) {
                isoLength++;
            }
            String from = input.substring(0, isoLength);
            String to = input.substring(isoLength + 1);

            if (!tryParseDate(FORMATTER_ISO, from) && !tryParseDate(FORMATTER_ISO, to)) {
                throw new IllegalStateException("Neplatný interval: " + input);
            }

            unitDate.setValueFrom(from);
            unitDate.setValueFromEstimated(false);
            unitDate.setValueTo(to);
            unitDate.setValueToEstimated(false);
        }
        normalize(unitDate);

        return unitDate;
    }
}
