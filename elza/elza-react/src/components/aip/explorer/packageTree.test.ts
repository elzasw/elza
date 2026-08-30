import { describe, it, expect } from 'vitest';

import { buildPackageTree, folderPaths } from './packageTree';

const entry = (path: string, size = 1) => ({path, size});

describe('buildPackageTree', () => {
    it('poskládá strom z plochých cest', () => {
        const tree = buildPackageTree([
            entry('aip/METS.xml', 10),
            entry('aip/data/scan.jpg', 20),
            entry('aip/metadata/PREMIS.xml', 30),
        ]);

        expect(tree).toHaveLength(1);
        expect(tree[0].path).toBe('aip');
        expect(tree[0].children?.map(n => n.name)).toEqual(['data', 'metadata', 'METS.xml']);

        const data = tree[0].children?.find(n => n.name === 'data');
        expect(data?.children).toEqual([{name: 'scan.jpg', path: 'aip/data/scan.jpg', size: 20}]);
    });

    it('řadí složky před soubory a jinak podle jména', () => {
        const tree = buildPackageTree([
            entry('b.xml'),
            entry('a.xml'),
            entry('zz/inner.xml'),
        ]);

        expect(tree.map(n => n.name)).toEqual(['zz', 'a.xml', 'b.xml']);
    });

    it('zvládne soubor v kořeni balíčku', () => {
        const tree = buildPackageTree([entry('METS.xml', 5)]);

        expect(tree).toEqual([{name: 'METS.xml', path: 'METS.xml', size: 5}]);
    });

    it('prázdný balíček dá prázdný strom', () => {
        expect(buildPackageTree([])).toEqual([]);
    });

    it('vrátí cesty všech složek', () => {
        const tree = buildPackageTree([entry('aip/data/scan.jpg'), entry('aip/METS.xml')]);

        expect(folderPaths(tree).sort()).toEqual(['aip', 'aip/data']);
    });
});
