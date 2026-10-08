package cz.tacr.elza.core.data;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.TranslationEntityType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves package-provided texts in a language.
 *
 * A text is looked up in the requested language, then in the same language without region
 * ({@code en-GB} -> {@code en}), and falls back to the source text - the text stored in the entity
 * itself, or for {@link TranslationEntityType#MESSAGE} the text in the source language of the
 * package that defines the message. A {@code null} language always gives the source text.
 *
 * <p>Methods without a language argument use the language of the current request, see
 * {@link #requestLanguage()}.
 */
@Service
public class PackageTexts {

    /**
     * Cookie with the UI language of the client (BCP 47 tag). The client sets it, so it reaches also
     * plain links (downloads, exports), which cannot carry a header.
     */
    public static final String LANGUAGE_COOKIE = "elza-lang";

    /** Request attribute caching the resolved language of the request. */
    private static final String REQUEST_LANGUAGE_ATTRIBUTE = PackageTexts.class.getName() + ".language";

    /** Value of {@link #REQUEST_LANGUAGE_ATTRIBUTE} for "no language" (source texts). */
    private static final Object NO_LANGUAGE = new Object();

    private final StaticDataService staticDataService;

    private final ElzaLocale elzaLocale;

    @Autowired
    public PackageTexts(StaticDataService staticDataService, ElzaLocale elzaLocale) {
        this.staticDataService = staticDataService;
        this.elzaLocale = elzaLocale;
    }

    /**
     * Language of the current request: the UI language from the {@link #LANGUAGE_COOKIE} cookie, else
     * the first UI language of the {@code Accept-Language} header, else the language of
     * {@code elza.locale}. Without an HTTP request (asynchronous work) the language of
     * {@code elza.locale}. Resolved once per request.
     *
     * @return language, null when even {@code elza.locale} names no known language (source texts)
     */
    public SysLanguage requestLanguage() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return defaultLanguage();
        }
        Object cached = attributes.getAttribute(REQUEST_LANGUAGE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (cached != null) {
            return cached == NO_LANGUAGE ? null : (SysLanguage) cached;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        SysLanguage language = resolveRequestLanguage(cookieValue(request, LANGUAGE_COOKIE));
        if (language == null) {
            language = resolveRequestLanguage(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
        }
        if (language == null) {
            language = defaultLanguage();
        }
        attributes.setAttribute(REQUEST_LANGUAGE_ATTRIBUTE, language != null ? language : NO_LANGUAGE,
                                RequestAttributes.SCOPE_REQUEST);
        return language;
    }

    /**
     * Tag of the language of the current request, see {@link #requestLanguage()}; null when there is
     * none (texts in the default language).
     */
    public String requestLanguageTag() {
        SysLanguage language = requestLanguage();
        return language != null ? language.getTag() : null;
    }

    /**
     * Language of {@code elza.locale}; a locale with a region falls back to its language.
     *
     * @return language, null when the locale names no known language
     */
    public SysLanguage defaultLanguage() {
        Locale locale = elzaLocale.getLocale();
        StaticDataProvider sdp = staticDataService.getData();
        SysLanguage language = sdp.getSysLanguageByTag(locale.toLanguageTag());
        if (language == null && !locale.getLanguage().isEmpty()) {
            language = sdp.getSysLanguageByTag(locale.getLanguage());
        }
        return language;
    }

    private static String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Language of a request: the first language of an {@code Accept-Language} header the UI can be
     * used in. A tag with a region matches the language without region when there is no exact
     * match.
     *
     * @param acceptLanguage
     *            value of the header, may be null
     * @return language, null when the header is empty or names no UI language
     */
    public SysLanguage resolveRequestLanguage(String acceptLanguage) {
        if (StringUtils.isBlank(acceptLanguage)) {
            return null;
        }
        List<Locale.LanguageRange> ranges;
        try {
            ranges = Locale.LanguageRange.parse(acceptLanguage);
        } catch (IllegalArgumentException e) {
            return null;
        }
        StaticDataProvider sdp = staticDataService.getData();
        for (Locale.LanguageRange range : ranges) {
            if (range.getWeight() <= 0) {
                continue;
            }
            for (String tag : withoutRegion(range.getRange())) {
                if ("*".equals(tag)) {
                    continue;
                }
                SysLanguage language = sdp.getSysLanguageByTag(tag);
                if (language != null && Boolean.TRUE.equals(language.getUiEnabled())) {
                    return language;
                }
            }
        }
        return null;
    }

    /**
     * @param sourceText
     *            text stored in the entity, returned when there is no translation
     * @return translated text, or the source text
     */
    public String text(TranslationEntityType entityType, String entityCode, String field, String sourceText,
                       SysLanguage language) {
        String translated = translate(entityType, entityCode, field, language);
        return translated != null ? translated : sourceText;
    }

    /**
     * Text in the language of the current request.
     *
     * @see #text(TranslationEntityType, String, String, String, SysLanguage)
     */
    public String text(TranslationEntityType entityType, String entityCode, String field, String sourceText) {
        return text(entityType, entityCode, field, sourceText, requestLanguage());
    }

    /**
     * Name (field {@code name}) in the language of the current request.
     */
    public String name(TranslationEntityType entityType, String entityCode, String sourceName) {
        return text(entityType, entityCode, TranslationEntityType.NAME, sourceName, requestLanguage());
    }

    public String name(RulItemType itemType) {
        return name(itemType, requestLanguage());
    }

    public String shortcut(RulItemType itemType) {
        return shortcut(itemType, requestLanguage());
    }

    public String description(RulItemType itemType) {
        return description(itemType, requestLanguage());
    }

    public String name(RulItemSpec itemSpec) {
        return name(itemSpec, requestLanguage());
    }

    public String shortcut(RulItemSpec itemSpec) {
        return shortcut(itemSpec, requestLanguage());
    }

    public String description(RulItemSpec itemSpec) {
        return description(itemSpec, requestLanguage());
    }

    public String name(RulItemType itemType, SysLanguage language) {
        return text(TranslationEntityType.ITEM_TYPE, itemType.getCode(), TranslationEntityType.NAME,
                    itemType.getName(), language);
    }

    public String shortcut(RulItemType itemType, SysLanguage language) {
        return text(TranslationEntityType.ITEM_TYPE, itemType.getCode(), TranslationEntityType.SHORTCUT,
                    itemType.getShortcut(), language);
    }

    public String description(RulItemType itemType, SysLanguage language) {
        return text(TranslationEntityType.ITEM_TYPE, itemType.getCode(), TranslationEntityType.DESCRIPTION,
                    itemType.getDescription(), language);
    }

    public String name(RulItemSpec itemSpec, SysLanguage language) {
        return text(TranslationEntityType.ITEM_SPEC, itemSpec.getCode(), TranslationEntityType.NAME,
                    itemSpec.getName(), language);
    }

    public String shortcut(RulItemSpec itemSpec, SysLanguage language) {
        return text(TranslationEntityType.ITEM_SPEC, itemSpec.getCode(), TranslationEntityType.SHORTCUT,
                    itemSpec.getShortcut(), language);
    }

    public String description(RulItemSpec itemSpec, SysLanguage language) {
        return text(TranslationEntityType.ITEM_SPEC, itemSpec.getCode(), TranslationEntityType.DESCRIPTION,
                    itemSpec.getDescription(), language);
    }

    /**
     * Message of a package in a language, with its {@link MessageFormat} placeholders filled.
     *
     * @param key
     *            {@code <PACKAGE>/<KEY>}
     * @return formatted message; the key itself when the message is not defined
     */
    public String message(String key, SysLanguage language, Object... args) {
        StaticDataProvider sdp = staticDataService.getData();
        PackageTranslations translations = sdp.getTranslations();

        for (SysLanguage candidate : fallbackChain(language)) {
            String pattern = translations.get(TranslationEntityType.MESSAGE, key, TranslationEntityType.TEXT,
                                              candidate.getLanguageId());
            if (pattern != null) {
                return format(pattern, candidate, args);
            }
        }

        RulPackage definingPackage = findDefiningPackage(sdp, key);
        if (definingPackage != null) {
            String pattern = translations.get(TranslationEntityType.MESSAGE, key, TranslationEntityType.TEXT,
                                              definingPackage.getLanguageId());
            if (pattern != null) {
                return format(pattern, sdp.getSysLanguageById(definingPackage.getLanguageId()), args);
            }
        }
        return key;
    }

    private String translate(TranslationEntityType entityType, String entityCode, String field,
                             SysLanguage language) {
        PackageTranslations translations = staticDataService.getData().getTranslations();
        for (SysLanguage candidate : fallbackChain(language)) {
            String value = translations.get(entityType, entityCode, field, candidate.getLanguageId());
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * The language and, for a tag with a region, the language without region.
     */
    private List<SysLanguage> fallbackChain(SysLanguage language) {
        if (language == null) {
            return List.of();
        }
        List<SysLanguage> chain = new ArrayList<>(2);
        chain.add(language);
        String tag = language.getTag();
        int dash = tag.indexOf('-');
        if (dash > 0) {
            SysLanguage base = staticDataService.getData().getSysLanguageByTag(tag.substring(0, dash));
            if (base != null) {
                chain.add(base);
            }
        }
        return chain;
    }

    /**
     * The tag and, for a tag with a region, the tag without region.
     */
    private static List<String> withoutRegion(String tag) {
        int dash = tag.indexOf('-');
        return dash > 0 ? List.of(tag, tag.substring(0, dash)) : List.of(tag);
    }

    private static RulPackage findDefiningPackage(StaticDataProvider sdp, String key) {
        int separator = key.indexOf(TranslationEntityType.CODE_SEPARATOR);
        if (separator <= 0) {
            return null;
        }
        String packageCode = key.substring(0, separator);
        return sdp.getPackages().stream()
                .filter(p -> packageCode.equals(p.getCode()))
                .findFirst()
                .orElse(null);
    }

    private static String format(String pattern, SysLanguage language, Object... args) {
        Locale locale = language != null ? Locale.forLanguageTag(language.getTag()) : Locale.ROOT;
        return new MessageFormat(pattern, locale).format(args);
    }
}
