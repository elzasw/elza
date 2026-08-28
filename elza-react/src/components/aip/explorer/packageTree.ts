import { AipPackageEntry } from 'elza-api';

/**
 * Uzel stromu balíčku. Balíček se přenáší jako plochý seznam cest, strom se z nich
 * skládá tady, aby prohlížeč nemusel řešit skládání cest při vykreslování.
 */
export type PackageNode = {
    /** Jméno položky v rámci nadřazené složky. */
    name: string;
    /** Celá cesta v balíčku; u složky cesta ke složce. */
    path: string;
    /** Soubory nemají potomky, složky ano. */
    children?: PackageNode[];
    /** Velikost souboru v bytech; u složky není. */
    size?: number;
};

const byFoldersFirstThenName = (a: PackageNode, b: PackageNode) => {
    const aFolder = a.children != null;
    const bFolder = b.children != null;
    if (aFolder !== bFolder) {
        return aFolder ? -1 : 1;
    }
    return a.name.localeCompare(b.name);
};

const sortTree = (nodes: PackageNode[]): PackageNode[] => {
    nodes.sort(byFoldersFirstThenName);
    nodes.forEach(node => {
        if (node.children) {
            sortTree(node.children);
        }
    });
    return nodes;
};

/**
 * Poskládá strom složek a souborů z plochého seznamu cest.
 */
export const buildPackageTree = (entries: AipPackageEntry[]): PackageNode[] => {
    const roots: PackageNode[] = [];
    const folders = new Map<string, PackageNode>();

    const folderAt = (path: string): PackageNode[] => {
        if (path === '') {
            return roots;
        }
        const existing = folders.get(path);
        if (existing) {
            return existing.children!;
        }
        const slash = path.lastIndexOf('/');
        const parent = folderAt(slash < 0 ? '' : path.substring(0, slash));
        const folder: PackageNode = {
            name: slash < 0 ? path : path.substring(slash + 1),
            path,
            children: [],
        };
        folders.set(path, folder);
        parent.push(folder);
        return folder.children!;
    };

    for (const entry of entries) {
        const path = entry.path.replace(/\/+$/, '');
        if (path === '') {
            continue;
        }
        const slash = path.lastIndexOf('/');
        const parent = folderAt(slash < 0 ? '' : path.substring(0, slash));
        parent.push({
            name: slash < 0 ? path : path.substring(slash + 1),
            path,
            size: entry.size,
        });
    }

    return sortTree(roots);
};

/** Cesty všech složek - strom se otevírá celý, balíčky jsou mělké. */
export const folderPaths = (nodes: PackageNode[]): string[] => {
    const paths: string[] = [];
    const walk = (items: PackageNode[]) => items.forEach(item => {
        if (item.children) {
            paths.push(item.path);
            walk(item.children);
        }
    });
    walk(nodes);
    return paths;
};
