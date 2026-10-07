import { describe, expect, it } from 'vitest';

import { RulPartTypeVO } from '../../api/RulPartTypeVO';
import { ApViewSettingRule } from '../../api/ApViewSettings';
import { sortPart } from './ApDetailPageWrapper';

/**
 * Part types follow the order of the rule set; a part type the rule set does not list is shown only
 * when the entity has parts of it.
 */

const partType = (id: number, code: string): RulPartTypeVO => ({ id, code, name: code, repeatable: true });

const items = [partType(1, 'PT_NAME'), partType(2, 'PT_BODY'), partType(3, 'PT_EVENT'), partType(4, 'PT_EXTRA')];

const rule = (codes: string[]): ApViewSettingRule => ({
    ruleSetId: 1,
    code: 'RS',
    partsOrder: codes.map(code => ({ code })),
    itemTypes: [],
});

describe('sortPart', () => {
    it('keeps all part types without an order', () => {
        expect(sortPart(items, undefined).map(p => p.code)).toEqual(['PT_NAME', 'PT_BODY', 'PT_EVENT', 'PT_EXTRA']);
        expect(sortPart(items, rule([])).map(p => p.code)).toEqual(['PT_NAME', 'PT_BODY', 'PT_EVENT', 'PT_EXTRA']);
    });

    it('orders the listed part types and hides unlisted ones without parts', () => {
        const sorted = sortPart(items, rule(['PT_EVENT', 'PT_NAME']), () => false);
        expect(sorted.map(p => p.code)).toEqual(['PT_EVENT', 'PT_NAME']);
    });

    it('shows an unlisted part type with parts after the listed ones', () => {
        const sorted = sortPart(items, rule(['PT_EVENT', 'PT_NAME']), id => id === 4);
        expect(sorted.map(p => p.code)).toEqual(['PT_EVENT', 'PT_NAME', 'PT_EXTRA']);
    });
});
