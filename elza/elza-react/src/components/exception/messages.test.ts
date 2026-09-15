import { describe, it, expect } from 'vitest';

import { expectTranslated } from 'test/i18n-catalog';
import { exceptionMessages, permissionNameMessages, hasExceptionMessage } from './messages';
import { exceptionMessageId } from './exceptionKey';

/**
 * Hlášky chyb se vybírají za běhu podle kódu ze serveru, takže je render
 * nikdy neprojde všechny - chybějící překlad by se ukázal až uživateli, a to
 * jen u té jedné chyby. Katalog se proto kontroluje přímo.
 */
describe('exceptionMessages', () => {
    it('má anglický překlad pro každou hlášku', () => {
        expectTranslated(Object.keys(exceptionMessages));
    });

    it('má anglický překlad pro každý název oprávnění', () => {
        expectTranslated(Object.keys(permissionNameMessages));
    });

    it('id deskriptoru se shoduje s klíčem v mapě', () => {
        for (const [key, descriptor] of Object.entries(exceptionMessages)) {
            expect(descriptor.id).toBe(key);
        }
    });

    /**
     * V ICU otevírá apostrof doslovný literál, takže `'{code}'` by se vypsalo
     * jako text `{code}` a hodnota by se nikdy nedosadila - bez jakékoli chyby.
     */
    it('nemá placeholder uzavřený v apostrofech', () => {
        const apostrophe = String.fromCharCode(39);
        const trap = new RegExp(`${apostrophe}\\{[^}]+\\}${apostrophe}`);
        const offenders = Object.entries(exceptionMessages)
            .filter(([, descriptor]) => trap.test(descriptor.defaultMessage ?? ''))
            .map(([id]) => id);
        expect(offenders).toEqual([]);
    });
});

describe('exceptionMessageId', () => {
    it('složí id ze skupiny a kódu', () => {
        expect(exceptionMessageId({ type: 'BaseCode', code: 'INSUFFICIENT_PERMISSIONS' })).toBe(
            'exception.base.INSUFFICIENT_PERMISSIONS',
        );
    });

    it('vrátí null pro neznámý typ', () => {
        expect(exceptionMessageId({ type: 'SomethingElse', code: 'X' })).toBeNull();
    });

    it('vrátí null bez kódu', () => {
        expect(exceptionMessageId({ type: 'BaseCode' })).toBeNull();
    });

    it('pozná, že hláška pro složené id existuje', () => {
        const id = exceptionMessageId({ type: 'BaseCode', code: 'INSUFFICIENT_PERMISSIONS' });
        expect(hasExceptionMessage(id!)).toBe(true);
        expect(hasExceptionMessage('exception.base.NEEXISTUJICI_KOD')).toBe(false);
    });
});
