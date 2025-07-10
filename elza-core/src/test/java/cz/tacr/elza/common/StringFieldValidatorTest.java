package cz.tacr.elza.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.validation.StringFieldValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes=StringFieldValidatorTest.AppConfig.class)
public class StringFieldValidatorTest {

	@Configuration
	static public class AppConfig {
		@Bean
	    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ConfigurableEnvironment env) {
	        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
	        MutablePropertySources propertySources = env.getPropertySources();

	        Map<String, Object> myMap = new HashMap<>();
	        myMap.put("elza.validate.stringfield.enabled", "true");

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

	@Test
	public void testArrDataString() {
		Set<ConstraintViolation<ArrDataString>> result;
        ArrDataString dataString = new ArrDataString();

        dataString.setStringValue(" abc ");
		result = validator.validate(dataString);
		assertEquals(1, result.size());
		assertEquals(StringFieldValidator.ERR_WHITESPACES, result.iterator().next().getMessage());

		dataString.setStringValue("qwerty\t\n\12345");
		result = validator.validate(dataString);
		assertEquals(1, result.size());
		assertEquals(StringFieldValidator.ERR_INVALID_CHRS, result.iterator().next().getMessage());

		dataString.setStringValue("qwerty  1234");
		result = validator.validate(dataString);
		assertEquals(1, result.size());
		assertEquals(StringFieldValidator.ERR_DOUBLE_SPCS, result.iterator().next().getMessage());

		dataString.setStringValue("");
		result = validator.validate(dataString);
		assertEquals(1, result.size());
		assertEquals(StringFieldValidator.ERR_BLANK_STR, result.iterator().next().getMessage());
	}

	@Test
	public void testArrDataText() {
		Set<ConstraintViolation<ArrDataText>> result;
		ArrDataText dataText = new ArrDataText();

		dataText.setTextValue(" abc ");
		result = validator.validate(dataText);
		assertEquals(1, result.size());
		assertEquals(StringFieldValidator.ERR_WHITESPACES, result.iterator().next().getMessage());

		// multiline for DataText is true
		dataText.setTextValue("qwerty\n\12345");
		result = validator.validate(dataText);
		assertEquals(0, result.size());

		dataText.setTextValue("qwerty\t\n\12345");
		result = validator.validate(dataText);
		assertEquals(1, result.size());
		assertEquals(StringFieldValidator.ERR_INVALID_CHRS, result.iterator().next().getMessage());
		
	    // double spaces are allowed on multiline text
		/*
		dataText.setTextValue("qwerty  12345");
		result = validator.validate(dataText);
		assertEquals(1, result.size());
		assertEquals(StringFieldValidator.ERR_DOUBLE_SPCS, result.iterator().next().getMessage());
		*/

		dataText.setTextValue("");
		result = validator.validate(dataText);
		assertEquals(1, result.size());
		assertEquals(StringFieldValidator.ERR_BLANK_STR, result.iterator().next().getMessage());
	}
}
