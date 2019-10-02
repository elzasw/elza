package cz.tacr.elza.core;

import java.text.Collator;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Helper service for locale
 */
@Service
public class ElzaLocale {

    /**
     * Elza locale
     */
    private Locale locale;

    @Autowired
    ElzaLocale(@Value("${elza.locale}") String languageTag) {
        this.locale = Locale.forLanguageTag(languageTag);
    }

    public Locale getLocale() {
        return locale;
    }

    public Collator getCollator() {
        return Collator.getInstance(locale);
    }
}
