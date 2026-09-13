import { describe, it, expect } from "vitest";

import { compareLegacy, countCalls, countTotal } from "./i18n-legacy-core.ts";

describe("countCalls", () => {
    it("počítá volání legacy helperu", () => {
        expect(countCalls("i18n('a') + i18n('b')")).toBe(2);
    });

    it("nezapočítá jiné identifikátory končící na i18n", () => {
        expect(countCalls("existsI18n('a')")).toBe(0);
        expect(countCalls("myI18n('a')")).toBe(0);
    });

    it("započítá i volání za tečkou nebo v JSX", () => {
        expect(countCalls("<span>{i18n('a')}</span>")).toBe(1);
    });

    it("vrací nulu, když soubor helper nepoužívá", () => {
        expect(countCalls("const x = 1;")).toBe(0);
    });
});

describe("compareLegacy", () => {
    it("projde, když počty sedí s baseline", () => {
        const result = compareLegacy({ "a.tsx": 3 }, { "a.tsx": 3 });
        expect(result.added).toEqual([]);
        expect(result.improved).toEqual([]);
        expect(result.total).toBe(3);
    });

    it("hlásí soubor, kde volání přibyla", () => {
        const result = compareLegacy({ "a.tsx": 4 }, { "a.tsx": 3 });
        expect(result.added).toEqual([{ file: "a.tsx", baseline: 3, actual: 4 }]);
    });

    it("hlásí nový soubor s legacy voláním, i když v baseline není", () => {
        const result = compareLegacy({ "new.tsx": 1 }, {});
        expect(result.added).toEqual([{ file: "new.tsx", baseline: 0, actual: 1 }]);
    });

    it("nezamaskuje přírůstek v jednom souboru úbytkem v jiném", () => {
        const result = compareLegacy({ "a.tsx": 0, "b.tsx": 8 }, { "a.tsx": 5, "b.tsx": 3 });
        expect(result.added).toEqual([{ file: "b.tsx", baseline: 3, actual: 8 }]);
    });

    it("označí úbytek jako zlepšení", () => {
        const result = compareLegacy({ "a.tsx": 1 }, { "a.tsx": 3 });
        expect(result.improved).toEqual([{ file: "a.tsx", baseline: 3, actual: 1 }]);
    });

    it("smazaný (nebo zcela vyčištěný) soubor je také zlepšení", () => {
        const result = compareLegacy({}, { "gone.tsx": 2 });
        expect(result.improved).toEqual([{ file: "gone.tsx", baseline: 2, actual: 0 }]);
        expect(result.added).toEqual([]);
    });
});

describe("countTotal", () => {
    it("sečte počty přes soubory", () => {
        expect(countTotal({ "a.tsx": 2, "b.tsx": 3 })).toBe(5);
    });
});
