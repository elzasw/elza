package cz.tacr.elza.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulTranslation;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.TranslationEntityType;

/**
 * Resolution of package texts without a database: precedence of packages, fallback from a
 * language with region, source texts and message formatting.
 */
class PackageTextsTest {

    private final SysLanguage cs = language(1, "cs", true);
    private final SysLanguage en = language(2, "en", true);
    private final SysLanguage enGb = language(3, "en-GB", true);
    private final SysLanguage de = language(4, "de", false);
    private final Map<String, SysLanguage> byTag = Map.of("cs", cs, "en", en, "en-gb", enGb, "de", de);

    private final RulPackage base = pkg(10, "BASE", cs);
    private final RulPackage addon = pkg(11, "ADDON", cs);
    private final RulPackage other = pkg(12, "OTHER", en);

    private StaticDataProvider sdp;
    private PackageTexts texts;

    @BeforeEach
    void setUp() {
        StaticDataService sds = mock(StaticDataService.class);
        sdp = mock(StaticDataProvider.class);
        lenient().when(sds.getData()).thenReturn(sdp);
        lenient().when(sdp.getSysLanguageByTag(anyString()))
                .thenAnswer(i -> byTag.get(((String) i.getArgument(0)).toLowerCase(Locale.ROOT)));
        lenient().when(sdp.getSysLanguageById(1)).thenReturn(cs);
        lenient().when(sdp.getSysLanguageById(2)).thenReturn(en);
        lenient().when(sdp.getPackages()).thenReturn(List.of(base, addon, other));

        // ADDON depends on BASE; OTHER is unrelated to both and its code sorts after them
        RulPackageDependency dependency = new RulPackageDependency();
        dependency.setRulPackage(addon);
        dependency.setDependsOnPackage(base);
        PackageTranslations translations = PackageTranslations.build(List.of(
                row(addon, "ITEM_TYPE", "T1", "name", en, "addon"),
                row(base, "ITEM_TYPE", "T1", "name", en, "base"),
                row(base, "ITEM_TYPE", "T2", "name", en, "base T2"),
                row(base, "ITEM_TYPE", "T2", "name", enGb, "british T2"),
                row(other, "ITEM_TYPE", "T3", "name", en, "other"),
                row(base, "ITEM_TYPE", "T3", "name", en, "base T3"),
                row(base, "MESSAGE", "BASE/HELLO", "text", cs, "Ahoj {0}, je {1,number,integer} hodin."),
                row(base, "MESSAGE", "BASE/HELLO", "text", en, "Hello {0}, it''s {1,number,integer} o''clock."),
                row(base, "MESSAGE", "CORE/AP_DUPLICATE_KEY_VALUE", "text", en, "Overridden by BASE"),
                row(base, "PART_TYPE", "PT_NAME", "name", en, "Name")),
                List.of(base, addon, other),
                List.of(dependency),
                CoreTranslations.load(List.of(cs, en)));
        lenient().when(sdp.getTranslations()).thenReturn(translations);

        // reference data named by the arguments of validation messages
        RulPartType partType = new RulPartType();
        partType.setCode("PT_NAME");
        partType.setName("Označení");
        lenient().when(sdp.getPartTypeByCode("PT_NAME")).thenReturn(partType);
        RulItemType rulItemType = new RulItemType();
        rulItemType.setCode("T1");
        rulItemType.setName("source");
        ItemType itemType = mock(ItemType.class);
        lenient().when(itemType.getEntity()).thenReturn(rulItemType);
        lenient().when(sdp.getItemTypeByCode("T1")).thenReturn(itemType);

        ElzaLocale elzaLocale = mock(ElzaLocale.class);
        lenient().when(elzaLocale.getLocale()).thenReturn(Locale.forLanguageTag("cs-CZ"));
        texts = new PackageTexts(sds, elzaLocale);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void dependentPackageWins() {
        assertEquals("addon", text("T1", en));
    }

    @Test
    void unrelatedPackagesAreOrderedByCode() {
        assertEquals("other", text("T3", en));
    }

    @Test
    void regionFallsBackToLanguage() {
        assertEquals("british T2", text("T2", enGb));
        assertEquals("addon", text("T1", enGb));
    }

    @Test
    void missingTranslationOrNoLanguageGivesSource() {
        assertEquals("source", text("T1", cs));
        assertEquals("source", text("T1", null));
        assertEquals("source", text("T9", en));
    }

    @Test
    void requestLanguageIsAUiLanguage() {
        assertEquals(enGb, texts.resolveRequestLanguage("en-GB"));
        assertEquals(en, texts.resolveRequestLanguage("en-US,cs;q=0.5"));
        assertEquals(cs, texts.resolveRequestLanguage("de,cs;q=0.5"));
        assertNull(texts.resolveRequestLanguage("de"));
        assertNull(texts.resolveRequestLanguage("cs;q=0"));
        assertNull(texts.resolveRequestLanguage(""));
    }

    /**
     * The cookie of the client wins over the browser header; without both the language of
     * {@code elza.locale} (with region, falling back to the language) is used, also outside a request.
     */
    @Test
    void requestLanguageComesFromCookieHeaderOrDefault() {
        assertEquals(cs, texts.requestLanguage());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-GB,en;q=0.9");
        request.setCookies(new jakarta.servlet.http.Cookie(PackageTexts.LANGUAGE_COOKIE, "cs"));
        bind(request);
        assertEquals(cs, texts.requestLanguage());

        request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-GB,en;q=0.9");
        bind(request);
        assertEquals(enGb, texts.requestLanguage());
        assertEquals("british T2", texts.name(TranslationEntityType.ITEM_TYPE, "T2", "source"));

        // a cookie naming no UI language is ignored
        request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(PackageTexts.LANGUAGE_COOKIE, "de"));
        bind(request);
        assertEquals(cs, texts.requestLanguage());
        assertEquals("source", texts.name(TranslationEntityType.ITEM_TYPE, "T1", "source"));
    }

    @Test
    void messageIsFormattedWithArguments() {
        assertEquals("Hello Jana, it's 7 o'clock.", texts.message("BASE/HELLO", en, "Jana", 7));
        assertEquals("Hello Jana, it's 7 o'clock.", texts.message("BASE/HELLO", enGb, "Jana", 7));
        assertEquals("Ahoj Jana, je 7 hodin.", texts.message("BASE/HELLO", de, "Jana", 7));
        assertEquals("Ahoj Jana, je 7 hodin.", texts.message("BASE/HELLO", null, "Jana", 7));
        assertEquals("BASE/UNKNOWN", texts.message("BASE/UNKNOWN", en));
        assertEquals("NO_PREFIX", texts.message("NO_PREFIX", en));
    }

    /**
     * A message stored by a validation: the translation row wins, then the text stored with the
     * message (the text of the rule); a plain line is kept.
     */
    @Test
    void storedMessageIsRenderedFromTranslationOrItsOwnText() {
        String line = ValidationMessage.of("BASE/HELLO", "Default {0}", "Jana", 7).encode();
        assertEquals("Hello Jana, it's 7 o'clock.", texts.render(line, en));
        assertEquals("Ahoj Jana, je 7 hodin.", texts.render(line, cs));

        line = ValidationMessage.of("BASE/ONLY_IN_RULE", "Only in the rule: {0}", "x").encode();
        assertEquals("Only in the rule: x", texts.render(line, en));
        assertEquals("Only in the rule: x", texts.render(line, null));
        assertEquals("BASE/UNKNOWN", texts.render(ValidationMessage.of("BASE/UNKNOWN", null).encode(), en));

        assertEquals("Plain {0} text", texts.render("Plain {0} text", en));
        assertEquals("Broken {0", texts.render(ValidationMessage.of("BASE/BAD", "Broken {0", "x").encode(), en));

        assertEquals("plain\nOnly in the rule: x\n",
                     texts.renderLines("plain\n" + ValidationMessage.of("BASE/ONLY_IN_RULE", "Only in the rule: {0}", "x").encode() + "\n"));
        assertNull(texts.renderLines(null));
    }

    /**
     * A message of the core comes from the translations shipped with the core in the language,
     * a package may override it, and without any translation the text of the code is shown.
     * Arguments naming item types and part types are rendered as their names.
     */
    @Test
    void coreMessageComesFromTheShippedTranslationsAndNamesItsArguments() {
        ValidationMessage duplicate = CoreMessage.AP_DUPLICATE_KEY_VALUE.with();
        assertEquals("CORE/AP_DUPLICATE_KEY_VALUE", duplicate.getKey());
        assertEquals(CoreMessage.AP_DUPLICATE_KEY_VALUE.text(), duplicate.getText());
        assertEquals("Overridden by BASE", texts.render(duplicate.encode(), en));
        assertEquals("Overridden by BASE", texts.render(duplicate.encode(), enGb));
        assertEquals("Duplicitní key value přístupového bodu.", texts.render(duplicate.encode(), cs));
        assertEquals("Duplicate key value of the entity.", texts.render(duplicate.encode(), de));
        assertEquals("Duplicate key value of the entity.", texts.render(duplicate.encode(), null));

        String missing = CoreMessage.AP_MISSING_REQUIRED_ITEM.with(ValidationMessage.partType("PT_NAME"),
                                                                   "T1", ValidationMessage.itemType("T1")).encode();
        assertEquals("The part Name is missing the required item T1 - addon", texts.render(missing, en));
        assertEquals("V části Označení chybí povinný typ prvku T1 - source", texts.render(missing, cs));

        // an unknown reference is rendered as its code
        String unknown = CoreMessage.AP_INVALID_ENTITY_REF.with(ValidationMessage.partType("PT_NOPE")).encode();
        assertEquals("The part PT_NOPE refers to an invalidated entity", texts.render(unknown, en));

        // a term of the core as an argument is rendered in the language too
        String undefined = CoreMessage.ARR_UNDEFINED_NOT_ALLOWED
                .with(ValidationMessage.itemType("T1"), CoreMessage.UNDEFINED_VALUE.with()).encode();
        assertEquals("The item addon cannot have the value “undefined”.", texts.render(undefined, en));
        assertEquals("U prvku popisu source není možné nastavit hodnotu „výjimka“.", texts.render(undefined, cs));
    }

    /**
     * A message of the core built without the service (the validator of funds), its source text
     * for the index, and the form stored in a column that cannot hold the encoded message.
     */
    @Test
    void coreMessageHasASourceTextAndFitsAColumn() {
        ValidationMessage missing = CoreMessage.ARR_MISSING_ITEM.with(ValidationMessage.itemType("T1"));
        assertEquals("CORE/ARR_MISSING_ITEM", missing.getKey());
        assertEquals("The item {0} must be filled in.", missing.getText());
        assertEquals("The item T1 must be filled in.", missing.sourceText());
        assertEquals("The item T1 must be filled in.", ValidationMessage.indexText(missing.encode()));
        assertEquals("plain", ValidationMessage.indexText("plain"));
        assertEquals("The item addon must be filled in.", texts.render(missing.encode(), en));
        assertEquals("Prvek source musí být vyplněn.", texts.render(missing.encode(), cs));

        String encoded = missing.encode();
        assertEquals(encoded, missing.storable(encoded.length()));
        assertEquals("The item T1 must be filled in.", missing.storable(encoded.length() - 1));
        assertEquals("The item", missing.storable(8));
        assertEquals("BASE/UNKNOWN", ValidationMessage.of("BASE/UNKNOWN", null).sourceText());
    }

    @Test
    void messageIsEncodedAsOneLine() {
        ValidationMessage message = ValidationMessage.of("BASE/KEY", "Text \"quoted\" {0}", "a\nb", null, 3);
        String line = message.encode();
        assertTrue(ValidationMessage.isEncoded(line));
        assertEquals(-1, line.indexOf('\n'));
        assertEquals(message, ValidationMessage.decode(line));
        assertEquals(List.of("a\nb", "", 3), ValidationMessage.decode(line).getArgs());
        assertNull(ValidationMessage.decode("plain"));
        assertNull(ValidationMessage.decode("{\"key\":broken"));

        assertEquals("BASE/KEY", ValidationMessage.qualify("BASE", "KEY"));
        assertEquals("OTHER/KEY", ValidationMessage.qualify("BASE", "OTHER/KEY"));
        assertEquals("KEY", ValidationMessage.qualify(null, "KEY"));
    }

    /**
     * The objects of the rules and of the domain become references, references and nested
     * messages survive the encoding, and a number keeps its digits under a bare placeholder.
     */
    @Test
    void argumentsAreTypedAndRenderedByKind() {
        RulItemType rulItemType = new RulItemType();
        rulItemType.setCode("T1");
        RulPartType partType = new RulPartType();
        partType.setCode("PT_NAME");
        ValidationMessage nested = ValidationMessage.of("BASE/HELLO", null, "Jana", 7);
        ValidationMessage message = ValidationMessage.of("BASE/TYPED", "{0} | {1} | {2} | {3} | {4,number,integer} | {5}",
                                                         rulItemType, partType, nested, 12383, 12383, true);

        List<Object> args = message.getArgs();
        assertEquals(ValidationMessage.itemType("T1"), args.get(0));
        assertEquals(ValidationMessage.partType("PT_NAME"), args.get(1));
        assertEquals(nested, args.get(2));
        assertEquals(12383, args.get(3));
        assertEquals(true, args.get(5));
        String line = message.encode();
        assertTrue(line.contains("{\"t\":\"ITEM_TYPE\",\"v\":\"T1\"}"), line);
        assertEquals(message, ValidationMessage.decode(line));

        assertEquals("addon | Name | Hello Jana, it's 7 o'clock. | 12383 | 12,383 | true", texts.render(line, en));
        // Czech groups digits with a non-breaking space
        assertEquals("source | Označení | Ahoj Jana, je 7 hodin. | 12383 | 12 383 | true", texts.render(line, cs));
        assertEquals("T1 | PT_NAME | BASE/HELLO | 12383 | 12,383 | true", message.sourceText());

        // an entity of a kind without a name here, or unknown: a translation row, else the code
        assertEquals("Name", texts.name(ValidationMessage.partType("PT_NAME"), en));
        assertEquals("PT_NOPE", texts.name(ValidationMessage.partType("PT_NOPE"), en));
        assertEquals("X", texts.name(new ValidationMessage.Ref("NO_SUCH_KIND", "X"), en));
    }

    @Test
    void dependencyDepthCountsTheLongestChain() {
        RulPackageDependency ab = dependency(base, addon);
        RulPackageDependency bc = dependency(addon, other);
        RulPackageDependency ac = dependency(base, other);
        Map<Integer, Integer> depth = PackageTranslations.dependencyDepth(List.of(ab, bc, ac));
        assertEquals(0, depth.get(other.getPackageId()));
        assertEquals(1, depth.get(addon.getPackageId()));
        assertEquals(2, depth.get(base.getPackageId()));
    }

    /**
     * Diamond: LEFT and RIGHT both depend on BASE (equal depth, the later code wins), TOP depends on
     * both and wins over them.
     */
    @Test
    void diamondTieIsDecidedByCodeAndTheTopWins() {
        RulPackage left = pkg(20, "LEFT", cs);
        RulPackage right = pkg(21, "RIGHT", cs);
        RulPackage top = pkg(22, "TOP", cs);
        List<RulPackageDependency> deps = List.of(dependency(left, base), dependency(right, base),
                                                  dependency(top, left), dependency(top, right));
        List<RulPackage> packages = List.of(base, left, right, top);

        PackageTranslations tie = PackageTranslations.build(List.of(
                row(left, "ITEM_TYPE", "T1", "name", en, "left"),
                row(right, "ITEM_TYPE", "T1", "name", en, "right"),
                row(base, "ITEM_TYPE", "T1", "name", en, "base")), packages, deps);
        lenient().when(sdp.getTranslations()).thenReturn(tie);
        assertEquals("right", text("T1", en));

        PackageTranslations withTop = PackageTranslations.build(List.of(
                row(left, "ITEM_TYPE", "T1", "name", en, "left"),
                row(top, "ITEM_TYPE", "T1", "name", en, "top"),
                row(right, "ITEM_TYPE", "T1", "name", en, "right")), packages, deps);
        lenient().when(sdp.getTranslations()).thenReturn(withTop);
        assertEquals("top", text("T1", en));
    }

    /**
     * A dependent package may override a text of its dependency in the dependency's own source
     * language; the override wins over the source text.
     */
    @Test
    void sameLanguageOverrideWins() {
        RulPackageDependency dependency = dependency(addon, base);
        PackageTranslations overridden = PackageTranslations.build(List.of(
                row(addon, "ITEM_TYPE", "T1", "name", cs, "přejmenováno")),
                List.of(base, addon, other), List.of(dependency));
        lenient().when(sdp.getTranslations()).thenReturn(overridden);
        assertEquals("přejmenováno", text("T1", cs));
        assertEquals("source", text("T1", en));
    }

    private static void bind(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private String text(String code, SysLanguage language) {
        return texts.text(TranslationEntityType.ITEM_TYPE, code, TranslationEntityType.NAME, "source", language);
    }

    private static SysLanguage language(int id, String tag, boolean ui) {
        SysLanguage l = new SysLanguage();
        l.setLanguageId(id);
        l.setTag(tag);
        l.setUiEnabled(ui);
        l.setScopeEnabled(true);
        return l;
    }

    private static RulPackage pkg(int id, String code, SysLanguage language) {
        RulPackage p = new RulPackage();
        p.setPackageId(id);
        p.setCode(code);
        p.setLanguage(language);
        return p;
    }

    /** {@code from} depends on {@code to}. */
    private static RulPackageDependency dependency(RulPackage from, RulPackage to) {
        RulPackageDependency d = new RulPackageDependency();
        d.setRulPackage(from);
        d.setDependsOnPackage(to);
        return d;
    }

    private static RulTranslation row(RulPackage p, String type, String code, String field, SysLanguage language,
                                      String value) {
        RulTranslation t = new RulTranslation();
        t.setRulPackage(p);
        t.setEntityType(type);
        t.setEntityCode(code);
        t.setField(field);
        t.setLanguage(language);
        t.setTextValue(value);
        return t;
    }
}
