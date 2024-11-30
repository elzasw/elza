package cz.tacr.elza.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.validation.UnitDateValidator;
import cz.tacr.elza.validation.impl.ArrDescItemsPostValidatorImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.metadata.BeanDescriptor;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes=UnitDateValidatorTest.AppConfig.class)
public class UnitDateValidatorTest {
	
	@Configuration
	@ComponentScan(basePackageClasses = {cz.tacr.elza.validation.UnitDateValidator.class}, 
	excludeFilters = @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE , value=ArrDescItemsPostValidatorImpl.class))
	static public class AppConfig {
		@Bean
	    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ConfigurableEnvironment env) {
	        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
	        MutablePropertySources propertySources = env.getPropertySources();

	        Map<String, Object> myMap = new HashMap<>();
	        myMap.put("elza.validate.unitdate.enabled", "true");

	        propertySources.addFirst(new MapPropertySource("YOUR_MAP", myMap));
	        return configurer;
	    }
		
		 @Bean
		 public LocalValidatorFactoryBean validator() {
			 return new LocalValidatorFactoryBean();
		 }
	}
	
	@Autowired
	private Validator validator;
	
	//ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
	//Validator validator = validatorFactory.getValidator();	

	@Test
	public void testCentury() {
		BeanDescriptor unitDateValidator = validator.getConstraintsForClass(ArrDataUnitdate.class);
		assertNotNull(unitDateValidator);
		
		ArrDataUnitdate century = new ArrDataUnitdate();
		century.setValueFrom("2001-01-01T00:00:00");
		century.setFormat("C");
		century.setValueTo("2100-12-31T23:59:59");
		assertEquals( 0, validator.validate(century).size());

		ArrDataUnitdate centuryBc = new ArrDataUnitdate();
		centuryBc.setValueFrom("-0099-01-01T00:00:00");
		centuryBc.setFormat("C");
		centuryBc.setValueTo("0000-12-31T23:59:59");
		assertEquals( 0, validator.validate(centuryBc).size());

		ArrDataUnitdate centuryInt = new ArrDataUnitdate();
		centuryInt.setValueFrom("1801-01-01T00:00:00");
		centuryInt.setFormat("C-C");
		centuryInt.setValueTo("2100-12-31T23:59:59");
		assertEquals( 0, validator.validate(centuryInt).size());

		ArrDataUnitdate centuryIntErr1 = new ArrDataUnitdate();
		centuryIntErr1.setValueFrom("1801-01-01T00:00:00");
		centuryIntErr1.setFormat("C-C");
		centuryIntErr1.setValueTo("1600-12-31T23:59:59");
		assertEquals( 1, validator.validate(centuryIntErr1).size());

		ArrDataUnitdate century2 = new ArrDataUnitdate();
		century2.setValueFrom("2000-01-01T00:00:00");
		century2.setFormat("C");
		century2.setValueTo("2100-12-31T23:59:59");
		assertEquals( 1, validator.validate(century2).size());

		ArrDataUnitdate century3 = new ArrDataUnitdate();
		century3.setValueFrom("2001-01-01T00:00:00");
		century3.setFormat("C");
		century3.setValueTo("2099-12-31T23:59:59");
		assertEquals( 1, validator.validate(century3).size());

		ArrDataUnitdate century4 = new ArrDataUnitdate();
		century4.setValueFrom("-0099-01-01T00:00:00");
		century4.setFormat("C");
		century4.setValueTo("0100-12-31T23:59:59");
		assertEquals( 1, validator.validate(century4).size());
	}

	@Test
	public void testYear() {
		BeanDescriptor unitDateValidator = validator.getConstraintsForClass(ArrDataUnitdate.class);
		assertNotNull(unitDateValidator);

		ArrDataUnitdate year = new ArrDataUnitdate();
		year.setValueFrom("2024-01-01T00:00:00");
		year.setFormat("Y");
		year.setValueTo("2024-12-31T23:59:59");
		assertEquals( 0, validator.validate(year).size());

		ArrDataUnitdate yearBc = new ArrDataUnitdate();
		yearBc.setValueFrom("-0098-01-01T00:00:00");
		yearBc.setFormat("Y");
		yearBc.setValueTo("-0098-12-31T23:59:59");
		assertEquals( 0, validator.validate(yearBc).size());

		ArrDataUnitdate yearInt = new ArrDataUnitdate();
		yearInt.setValueFrom("2023-01-01T00:00:00");
		yearInt.setFormat("Y-Y");
		yearInt.setValueTo("2024-12-31T23:59:59");
		assertEquals( 0, validator.validate(yearInt).size());
		
		ArrDataUnitdate yearErr1 = new ArrDataUnitdate();
		yearErr1.setValueFrom("2023-01-01T00:00:00");
		yearErr1.setFormat("Y");
		yearErr1.setValueTo("2024-12-31T23:59:59");
		assertEquals( 1, validator.validate(yearErr1).size());		

		ArrDataUnitdate yearErr2 = new ArrDataUnitdate();
		yearErr2.setValueFrom("2024-02-01T00:00:00");
		yearErr2.setFormat("Y");
		yearErr2.setValueTo("2024-12-31T23:59:59");
		assertEquals( 1, validator.validate(yearErr2).size());		

		ArrDataUnitdate yearErr3 = new ArrDataUnitdate();
		yearErr3.setValueFrom("2024-01-01T00:00:00");
		yearErr3.setFormat("Y");
		yearErr3.setValueTo("2024-12-30T23:59:59");
		assertEquals( 1, validator.validate(yearErr3).size());
		
		ArrDataUnitdate yearIntErr1 = new ArrDataUnitdate();
		yearIntErr1.setValueFrom("2024-01-01T00:00:00");
		yearIntErr1.setFormat("Y-Y");
		yearIntErr1.setValueTo("2024-12-31T23:59:59");
		assertEquals( 1, validator.validate(yearIntErr1).size());		
	}

	@Test
	public void testYearMonth() {
		ArrDataUnitdate yearMonth = new ArrDataUnitdate();
		yearMonth.setValueFrom("2024-03-01T00:00:00");
		yearMonth.setFormat("YM");
		yearMonth.setValueTo("2024-03-31T23:59:59");
		assertEquals( 0, validator.validate(yearMonth).size());

		ArrDataUnitdate yearMonth2 = new ArrDataUnitdate();
		yearMonth2.setValueFrom("2024-02-01T00:00:00");
		yearMonth2.setFormat("YM");
		yearMonth2.setValueTo("2024-02-29T23:59:59");
		assertEquals( 0, validator.validate(yearMonth2).size());

		ArrDataUnitdate yearMonth3 = new ArrDataUnitdate();
		yearMonth3.setValueFrom("2022-02-01T00:00:00");
		yearMonth3.setFormat("YM");
		yearMonth3.setValueTo("2022-02-28T23:59:59");
		assertEquals( 0, validator.validate(yearMonth3).size());

		ArrDataUnitdate yearMonthInt = new ArrDataUnitdate();
		yearMonthInt.setValueFrom("2024-03-01T00:00:00");
		yearMonthInt.setFormat("YM-YM");
		yearMonthInt.setValueTo("2024-04-30T23:59:59");
		assertEquals( 0, validator.validate(yearMonthInt).size());

		ArrDataUnitdate yearMonthInt2 = new ArrDataUnitdate();
		yearMonthInt2.setValueFrom("2024-02-01T00:00:00");
		yearMonthInt2.setFormat("YM-YM");
		yearMonthInt2.setValueTo("2025-02-28T23:59:59");
		assertEquals( 0, validator.validate(yearMonthInt2).size());

		ArrDataUnitdate yearMonthErr1 = new ArrDataUnitdate();
		yearMonthErr1.setValueFrom("2024-03-02T00:00:00");
		yearMonthErr1.setFormat("YM");
		yearMonthErr1.setValueTo("2024-03-31T23:59:59");
		assertEquals( 1, validator.validate(yearMonthErr1).size());

		ArrDataUnitdate yearMonthErr2 = new ArrDataUnitdate();
		yearMonthErr2.setValueFrom("2024-03-01T00:00:00");
		yearMonthErr2.setFormat("YM");
		yearMonthErr2.setValueTo("2024-03-28T23:59:59");
		assertEquals( 1, validator.validate(yearMonthErr2).size());

		ArrDataUnitdate yearMonthErr3 = new ArrDataUnitdate();
		yearMonthErr3.setValueFrom("2024-02-01T00:00:00");
		yearMonthErr3.setFormat("YM");
		yearMonthErr3.setValueTo("2024-02-28T23:59:59");
		assertEquals( 1, validator.validate(yearMonthErr3).size());

		ArrDataUnitdate yearMonthErr4 = new ArrDataUnitdate();
		yearMonthErr4.setValueFrom("2024-02-01T00:00:00");
		yearMonthErr4.setFormat("YM");
		yearMonthErr4.setValueTo("2024-03-31T23:59:59");
		assertEquals( 1, validator.validate(yearMonthErr4).size());

		ArrDataUnitdate yearMonthErr5 = new ArrDataUnitdate();
		yearMonthErr5.setValueFrom("2024-03-01T00:00:00");
		yearMonthErr5.setFormat("YM-YM");
		yearMonthErr5.setValueTo("2024-03-31T23:59:59");
		assertEquals( 1, validator.validate(yearMonthErr5).size());
	}

	@Test
	public void testDay() {
		ArrDataUnitdate day = new ArrDataUnitdate();
		day.setValueFrom("2024-03-04T00:00:00");
		day.setFormat("D");
		day.setValueTo("2024-03-04T23:59:59");
		assertEquals( 0, validator.validate(day).size());		

		ArrDataUnitdate day2 = new ArrDataUnitdate();
		day2.setValueFrom("-2024-03-04T00:00:00");
		day2.setFormat("D");
		day2.setValueTo("-2024-03-04T23:59:59");
		assertEquals( 0, validator.validate(day2).size());		

		ArrDataUnitdate dayErr1 = new ArrDataUnitdate();
		dayErr1.setValueFrom("2024-03-04T00:00:00");
		dayErr1.setFormat("D");
		dayErr1.setValueTo("2024-03-05T23:59:59");
		assertEquals( 1, validator.validate(dayErr1).size());		

		ArrDataUnitdate dayErr2 = new ArrDataUnitdate();
		dayErr2.setValueFrom("2024-03-04T01:00:00");
		dayErr2.setFormat("D");
		dayErr2.setValueTo("2024-03-04T23:59:59");
		assertEquals( 1, validator.validate(dayErr2).size());		

		ArrDataUnitdate dayErr3 = new ArrDataUnitdate();
		dayErr3.setValueFrom("2024-03-04T00:00:00");
		dayErr3.setFormat("D");
		dayErr3.setValueTo("2024-03-04T23:58:59");
		assertEquals( 1, validator.validate(dayErr3).size());		

		ArrDataUnitdate dayInt = new ArrDataUnitdate();
		dayInt.setValueFrom("2024-03-04T00:00:00");
		dayInt.setFormat("D-D");
		dayInt.setValueTo("2024-03-05T23:59:59");
		assertEquals( 0, validator.validate(dayInt).size());		

		ArrDataUnitdate dayIntErr1 = new ArrDataUnitdate();
		dayIntErr1.setValueFrom("2024-03-04T00:00:00");
		dayIntErr1.setFormat("D-D");
		dayIntErr1.setValueTo("2024-03-04T23:59:59");
		assertEquals( 1, validator.validate(dayIntErr1).size());		

		ArrDataUnitdate dayIntErr2 = new ArrDataUnitdate();
		dayIntErr2.setValueFrom("2024-03-04T00:00:00");
		dayIntErr2.setFormat("D-D");
		dayIntErr2.setValueTo("2024-03-05T22:59:59");
		assertEquals( 1, validator.validate(dayIntErr2).size());
		
		ArrDataUnitdate dayIntErr3 = new ArrDataUnitdate();
		dayIntErr3.setValueFrom("2024-03-04T00:00:00");
		dayIntErr3.setFormat("D-D");
		dayIntErr3.setValueTo("2024-02-06T22:59:59");
		assertEquals( 1, validator.validate(dayIntErr3).size());		
	}

	@Test
	public void testDayTime() {
		ArrDataUnitdate day = new ArrDataUnitdate();
		day.setValueFrom("2024-03-04T15:23:37");
		day.setFormat("DT");
		day.setValueTo("2024-03-04T15:23:37");
		assertEquals( 0, validator.validate(day).size());

		ArrDataUnitdate dayErr1 = new ArrDataUnitdate();
		dayErr1.setValueFrom("2024-03-04T15:23:37");
		dayErr1.setFormat("DT");
		dayErr1.setValueTo("2024-03-04T15:23:38");
		assertEquals( 1, validator.validate(dayErr1).size());

		ArrDataUnitdate dayErr2 = new ArrDataUnitdate();
		dayErr2.setValueFrom("2024-03-04T15:23:37");
		dayErr2.setFormat("DT");
		dayErr2.setValueTo("2024-03-04T15:24:37");
		assertEquals( 1, validator.validate(dayErr2).size());

		ArrDataUnitdate dayErr3 = new ArrDataUnitdate();
		dayErr3.setValueFrom("2024-03-04T15:23:37");
		dayErr3.setFormat("DT");
		dayErr3.setValueTo("2024-03-04T14:23:37");
		assertEquals( 1, validator.validate(dayErr3).size());

		ArrDataUnitdate dayErr4 = new ArrDataUnitdate();
		dayErr4.setValueFrom("2024-03-04T15:23:37");
		dayErr4.setFormat("DT");
		dayErr4.setValueTo("2024-03-03T15:23:37");
		assertEquals( 1, validator.validate(dayErr4).size());

		ArrDataUnitdate dayInt = new ArrDataUnitdate();
		dayInt.setValueFrom("2024-03-04T15:23:36");
		dayInt.setFormat("DT-DT");
		dayInt.setValueTo("2024-03-04T15:23:37");
		assertEquals( 0, validator.validate(dayInt).size());

		ArrDataUnitdate dayIntErr1 = new ArrDataUnitdate();
		dayIntErr1.setValueFrom("2024-03-04T15:23:36");
		dayIntErr1.setFormat("DT-DT");
		dayIntErr1.setValueTo("2024-03-04T15:23:36");
		assertEquals( 1, validator.validate(dayIntErr1).size());
		
		ArrDataUnitdate dayIntErr2 = new ArrDataUnitdate();
		dayIntErr2.setValueFrom("2024-03-04T15:23:36");
		dayIntErr2.setFormat("DT-DT");
		dayIntErr2.setValueTo("2024-03-04T15:22:37");
		assertEquals( 1, validator.validate(dayIntErr2).size());		
	}

	@Test
	public void testCombined() {
		ArrDataUnitdate centuryInt = new ArrDataUnitdate();
		centuryInt.setValueFrom("1801-01-01T00:00:00");
		centuryInt.setFormat("C-DT");
		centuryInt.setValueTo("1901-01-01T00:00:00");
		assertEquals( 0, validator.validate(centuryInt).size());		

		ArrDataUnitdate centuryIntErr1 = new ArrDataUnitdate();
		centuryIntErr1.setValueFrom("1801-01-01T00:00:00");
		centuryIntErr1.setFormat("C-DT");
		centuryIntErr1.setValueTo("1899-12-31T23:59:59");
		assertEquals( 1, validator.validate(centuryIntErr1).size());		

		ArrDataUnitdate centuryInt2 = new ArrDataUnitdate();
		centuryInt2.setValueFrom("1801-01-01T00:00:00");
		centuryInt2.setFormat("C-D");
		centuryInt2.setValueTo("1901-01-01T23:59:59");
		assertEquals( 0, validator.validate(centuryInt2).size());		

		ArrDataUnitdate centuryIntErr2 = new ArrDataUnitdate();
		centuryIntErr2.setValueFrom("1801-01-01T00:00:00");
		centuryIntErr2.setFormat("C-D");
		centuryIntErr2.setValueTo("1900-12-31T23:59:59");
		assertEquals( 1, validator.validate(centuryIntErr2).size());		

		ArrDataUnitdate centuryInt3 = new ArrDataUnitdate();
		centuryInt3.setValueFrom("1801-01-01T00:00:00");
		centuryInt3.setFormat("C-YM");
		centuryInt3.setValueTo("1901-01-31T23:59:59");
		assertEquals( 0, validator.validate(centuryInt3).size());		

		ArrDataUnitdate centuryIntErr3 = new ArrDataUnitdate();
		centuryIntErr3.setValueFrom("1801-01-01T00:00:00");
		centuryIntErr3.setFormat("C-YM");
		centuryIntErr3.setValueTo("1900-12-31T23:59:59");
		assertEquals( 1, validator.validate(centuryIntErr3).size());		

		ArrDataUnitdate centuryInt4 = new ArrDataUnitdate();
		centuryInt4.setValueFrom("1801-01-01T00:00:00");
		centuryInt4.setFormat("C-Y");
		centuryInt4.setValueTo("1901-12-31T23:59:59");
		assertEquals( 0, validator.validate(centuryInt4).size());		

		ArrDataUnitdate centuryIntErr4 = new ArrDataUnitdate();
		centuryIntErr4.setValueFrom("1801-01-01T00:00:00");
		centuryIntErr4.setFormat("C-Y");
		centuryIntErr4.setValueTo("1900-12-31T23:59:59");
		assertEquals( 1, validator.validate(centuryIntErr4).size());		

		ArrDataUnitdate yearInt = new ArrDataUnitdate();
		yearInt.setValueFrom("1821-01-01T00:00:00");
		yearInt.setFormat("Y-DT");
		yearInt.setValueTo("1822-01-01T00:00:00");
		assertEquals( 0, validator.validate(yearInt).size());		

		ArrDataUnitdate yearIntErr = new ArrDataUnitdate();
		yearIntErr.setValueFrom("1821-01-01T00:00:00");
		yearIntErr.setFormat("Y-DT");
		yearIntErr.setValueTo("1821-12-31T23:59:59");
		assertEquals( 1, validator.validate(yearIntErr).size());		

		ArrDataUnitdate yearInt2 = new ArrDataUnitdate();
		yearInt2.setValueFrom("1821-01-01T00:00:00");
		yearInt2.setFormat("Y-D");
		yearInt2.setValueTo("1822-01-01T23:59:59");
		assertEquals( 0, validator.validate(yearInt2).size());		

		ArrDataUnitdate yearIntErr2 = new ArrDataUnitdate();
		yearIntErr2.setValueFrom("1821-01-01T00:00:00");
		yearIntErr2.setFormat("Y-D");
		yearIntErr2.setValueTo("1821-12-31T23:59:59");
		assertEquals( 1, validator.validate(yearIntErr2).size());		

		ArrDataUnitdate yearInt3 = new ArrDataUnitdate();
		yearInt3.setValueFrom("1821-01-01T00:00:00");
		yearInt3.setFormat("Y-YM");
		yearInt3.setValueTo("1822-01-31T23:59:59");
		assertEquals( 0, validator.validate(yearInt3).size());		

		ArrDataUnitdate yearIntErr3 = new ArrDataUnitdate();
		yearIntErr3.setValueFrom("1821-01-01T00:00:00");
		yearIntErr3.setFormat("Y-YM");
		yearIntErr3.setValueTo("1821-12-31T23:59:59");
		assertEquals( 1, validator.validate(yearIntErr3).size());

		ArrDataUnitdate yearMonthInt = new ArrDataUnitdate();
		yearMonthInt.setValueFrom("1821-03-01T00:00:00");
		yearMonthInt.setFormat("YM-DT");
		yearMonthInt.setValueTo("1821-04-01T00:00:00");
		assertEquals( 0, validator.validate(yearMonthInt).size());
		
		ArrDataUnitdate yearMonthIntErr1 = new ArrDataUnitdate();
		yearMonthIntErr1.setValueFrom("1821-03-01T00:00:00");
		yearMonthIntErr1.setFormat("YM-DT");
		yearMonthIntErr1.setValueTo("1821-03-31T23:59:59");
		assertEquals( 1, validator.validate(yearMonthIntErr1).size());
		
		ArrDataUnitdate yearMonthInt2 = new ArrDataUnitdate();
		yearMonthInt2.setValueFrom("1821-03-01T00:00:00");
		yearMonthInt2.setFormat("YM-D");
		yearMonthInt2.setValueTo("1821-04-01T23:59:59");
		assertEquals( 0, validator.validate(yearMonthInt2).size());
		
		ArrDataUnitdate yearMonthIntErr2 = new ArrDataUnitdate();
		yearMonthIntErr2.setValueFrom("1821-03-01T00:00:00");
		yearMonthIntErr2.setFormat("YM-D");
		yearMonthIntErr2.setValueTo("1821-03-31T23:59:59");
		assertEquals( 1, validator.validate(yearMonthIntErr2).size());
		
		ArrDataUnitdate dayInt = new ArrDataUnitdate();
		dayInt.setValueFrom("1821-03-14T00:00:00");
		dayInt.setFormat("D-DT");
		dayInt.setValueTo("1821-03-15T00:00:00");
		assertEquals( 0, validator.validate(dayInt).size());
		
		ArrDataUnitdate dayIntErr1 = new ArrDataUnitdate();
		dayIntErr1.setValueFrom("1821-03-14T00:00:00");
		dayIntErr1.setFormat("D-DT");
		dayIntErr1.setValueTo("1821-03-14T23:59:59");
		assertEquals( 1, validator.validate(dayIntErr1).size());		
	}

	@Test
	public void testEstimated() {
		ArrDataUnitdate yearInt = new ArrDataUnitdate();
		yearInt.setValueFrom("2023-01-01T00:00:00");
		yearInt.setFormat("Y");
		yearInt.setValueTo("2023-12-31T23:59:59");
		yearInt.setValueFromEstimated(false);
		yearInt.setValueToEstimated(false);
		assertEquals( 0, validator.validate(yearInt).size());
		
		ArrDataUnitdate yearInt2 = new ArrDataUnitdate();
		yearInt2.setValueFrom("2023-01-01T00:00:00");
		yearInt2.setFormat("Y");
		yearInt2.setValueTo("2023-12-31T23:59:59");
		yearInt2.setValueFromEstimated(true);
		yearInt2.setValueToEstimated(true);
		assertEquals( 0, validator.validate(yearInt2).size());
		
		ArrDataUnitdate yearIntErr1 = new ArrDataUnitdate();
		yearIntErr1.setValueFrom("2023-01-01T00:00:00");
		yearIntErr1.setFormat("Y");
		yearIntErr1.setValueTo("2023-12-31T23:59:59");
		yearIntErr1.setValueFromEstimated(true);
		yearIntErr1.setValueToEstimated(false);
		assertEquals( 1, validator.validate(yearIntErr1).size());

		ArrDataUnitdate yearIntErr2 = new ArrDataUnitdate();
		yearIntErr2.setValueFrom("2023-01-01T00:00:00");
		yearIntErr2.setFormat("Y");
		yearIntErr2.setValueTo("2023-12-31T23:59:59");
		yearIntErr2.setValueFromEstimated(false);
		yearIntErr2.setValueToEstimated(true);
		assertEquals( 1, validator.validate(yearIntErr2).size());
	}
	
}