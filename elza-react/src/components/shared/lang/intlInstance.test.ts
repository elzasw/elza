import { describe, expect, it, vi } from "vitest";

import { createAppIntl, getIntl } from "./intlInstance";

const descriptor = { id: "test.missing", defaultMessage: "Výchozí text" };

describe("createAppIntl", () => {
    /**
     * Regrese: LangProvider volá createAppIntl bez onError. Dřív se do createIntl
     * dostal klíč `onError: undefined`, ten přepsal výchozí handler formatjs a
     * první chybějící překlad v jiném jazyce než cs shodil render
     * ("onError is not a function"). Přihlašovací stránka v EN tak vůbec nešla
     * zobrazit.
     */
    it("nespadne na chybějícím překladu, když volající nedodá onError", () => {
        const intl = createAppIntl("en", {});
        expect(() => intl.formatMessage(descriptor)).not.toThrow();
        expect(intl.formatMessage(descriptor)).toBe("Výchozí text");
    });

    it("mlčí o chybějícím překladu jen dokud katalog není načtený", () => {
        const spy = vi.spyOn(console, "error").mockImplementation(() => {});
        try {
            createAppIntl("en", {}).formatMessage(descriptor);
            expect(spy).not.toHaveBeenCalled();

            createAppIntl("en", { "some.other.key": "Loaded" }).formatMessage(descriptor);
            expect(spy).toHaveBeenCalledTimes(1);
            expect(spy.mock.calls[0]?.[0]).toMatchObject({ code: "MISSING_TRANSLATION" });
        } finally {
            spy.mockRestore();
        }
    });

    it("předá explicitní onError dál", () => {
        const onError = vi.fn();
        createAppIntl("en", {}, onError).formatMessage(descriptor);
        expect(onError).toHaveBeenCalledTimes(1);
    });

    it("getIntl vrací instanci z posledního createAppIntl", () => {
        const intl = createAppIntl("cs", { "test.missing": "Přeložený" });
        expect(getIntl()).toBe(intl);
        expect(getIntl().formatMessage(descriptor)).toBe("Přeložený");
    });
});
