import { describe, expect, it } from 'vitest';
import { AipLevelType } from 'elza-api';

import { renderWithProviders } from 'test/test-utils';
import { NamedNode, levelIcon, useNodeName } from './levels';

/**
 * Virtual levels of the tree are named by the client from their type; everything else keeps the
 * name the server sent. The server label stays as a fallback, so a type this client does not
 * know does not leave a node nameless.
 */

function Name({ node }: { node?: NamedNode | null }) {
    const nodeName = useNodeName();
    return <span data-testid="name">{nodeName(node)}</span>;
}

const name = (node?: NamedNode | null) => {
    const { container, unmount } = renderWithProviders(<Name node={node} />);
    const text = container.querySelector('[data-testid="name"]')?.textContent;
    unmount();
    return text;
};

describe('useNodeName', () => {
    it('virtuální úroveň pojmenuje podle typu, ne podle popisku ze serveru', () => {
        expect(name({ levelType: AipLevelType.Representations, label: 'cokoliv ze serveru' }))
            .toBe('Reprezentace');
    });

    it('neznámý typ úrovně nechá popisek ze serveru', () => {
        expect(name({ levelType: 'BRAND_NEW_LEVEL' as AipLevelType, label: 'Nová úroveň' }))
            .toBe('Nová úroveň');
    });

    it('reálné uzly si název nesou samy', () => {
        expect(name({ label: 'Reprezentace 1' })).toBe('Reprezentace 1');
        // strom logických kontejnerů pojmenovává uzly name, ne label
        expect(name({ name: 'Úroveň popisu' })).toBe('Úroveň popisu');
        expect(name({ filename: 'aip/metadata/PREMIS.xml' })).toBe('PREMIS.xml');
    });

    it('bez uzlu nespadne', () => {
        expect(name(null)).toBe('');
    });
});

describe('levelIcon', () => {
    it('dá ikonu každé úrovni stromu průzkumníka', () => {
        [AipLevelType.Package, AipLevelType.Representations, AipLevelType.LogicalStructure,
         AipLevelType.Metadata].forEach(levelType => {
            expect(levelIcon(levelType)).toBeDefined();
        });
    });

    it('reálný uzel i neznámý typ zůstávají bez ikony', () => {
        expect(levelIcon(undefined)).toBeUndefined();
        expect(levelIcon('BRAND_NEW_LEVEL' as AipLevelType)).toBeUndefined();
    });
});
