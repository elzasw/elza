package cz.tacr.elza.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.domain.RulPackage;
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
                row(base, "MESSAGE", "BASE/HELLO", "text", en, "Hello {0}, it''s {1,number,integer} o''clock.")),
                List.of(base, addon, other),
                List.of(dependency));
        lenient().when(sdp.getTranslations()).thenReturn(translations);

        texts = new PackageTexts(sds);
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

    @Test
    void messageIsFormattedWithArguments() {
        assertEquals("Hello Jana, it's 7 o'clock.", texts.message("BASE/HELLO", en, "Jana", 7));
        assertEquals("Hello Jana, it's 7 o'clock.", texts.message("BASE/HELLO", enGb, "Jana", 7));
        assertEquals("Ahoj Jana, je 7 hodin.", texts.message("BASE/HELLO", de, "Jana", 7));
        assertEquals("Ahoj Jana, je 7 hodin.", texts.message("BASE/HELLO", null, "Jana", 7));
        assertEquals("BASE/UNKNOWN", texts.message("BASE/UNKNOWN", en));
        assertEquals("NO_PREFIX", texts.message("NO_PREFIX", en));
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
