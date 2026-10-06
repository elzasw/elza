package cz.tacr.elza.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.Language;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.SysLanguage;

/**
 * Languages known to the installation ({@code sys_language}) and their usages.
 *
 * Implements the contract generated from {@code elza-openapi.yml} (tag {@code languages}).
 */
@RestController
@RequestMapping("/api/v1")
public class LanguagesController implements LanguagesApi {

    private final StaticDataService staticDataService;
    private final ElzaLocale elzaLocale;

    @Autowired
    public LanguagesController(StaticDataService staticDataService, ElzaLocale elzaLocale) {
        this.staticDataService = staticDataService;
        this.elzaLocale = elzaLocale;
    }

    /**
     * GET /languages
     * Returns all languages known to the installation with their usages, ordered by tag.
     * Public (the login page needs it); static data are bound to a transaction.
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<Language>> languagesListLanguages() {
        SysLanguage defaultLanguage = findDefaultLanguage();
        List<Language> result = staticDataService.getData().getSysLanguages().stream()
                .sorted(Comparator.comparing(SysLanguage::getTag))
                .map(l -> new Language(l.getTag(),
                        l.getCode(),
                        Boolean.TRUE.equals(l.getUiEnabled()),
                        Boolean.TRUE.equals(l.getScopeEnabled()),
                        l == defaultLanguage))
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Language of {@code elza.locale}; a locale with a region falls back to its language.
     */
    private SysLanguage findDefaultLanguage() {
        Locale locale = elzaLocale.getLocale();
        var sdp = staticDataService.getData();
        SysLanguage language = sdp.getSysLanguageByTag(locale.toLanguageTag());
        if (language == null && !locale.getLanguage().isEmpty()) {
            language = sdp.getSysLanguageByTag(locale.getLanguage());
        }
        return language;
    }
}
