export const getFileName = (name: string): string => {
    return name ? name.substring(name.lastIndexOf("/") + 1) : "-";
}

export const turncate = (str: string ): string => {
    if (str.length <= 20) {
        return str;
    }
    return str.slice(0, 17) + '...';
}

/** Uzel stromu AIP tak, jak ho vraci server; deti jsou v obou seznamech volitelne. */
export type ExplorerNode = {
    uuid?: string;
    daoFileFolderId?: number;
    parent?: ExplorerNode;
    childFiles?: ExplorerNode[];
    childFolders?: ExplorerNode[];
    daoId?: number;
    daoFileId?: number;
    filename?: string;
    label?: string;
    size?: number;
    mimeType?: string;
    [key: string]: unknown;
};

export type ExplorerNodeHit = {node: ExplorerNode; path: ExplorerNode[]};

export const findNodeByUUID = (
    tree: ExplorerNode | null | undefined,
    uuid: string | number,
    path: ExplorerNode[] = [],
): ExplorerNodeHit | null => {
    if (!tree) return null;

    path.push(tree);

    if (tree.uuid === uuid) {
        return { node: tree, path };
    }

    if (Array.isArray(tree.childFiles)) {
        for (const file of tree.childFiles) {
            file.parent = tree;
            if (file.uuid === uuid) {
                return { node: file, path: [...path, file] };
            }
        }
    }

    if (Array.isArray(tree.childFolders)) {
        for (const folder of tree.childFolders) {
            folder.parent = tree;
            const result = findNodeByUUID(folder, uuid, [...path]);
            if (result) {
                return result;
            }
        }
    }

    return null;
}

export const findNodeById = (
    tree: ExplorerNode | null | undefined,
    id: string | number,
    path: ExplorerNode[] = [],
): ExplorerNodeHit | null => {
    if (!tree) return null;

    path.push(tree);

    if (tree.daoFileFolderId === id ) {
        return { node: tree, path };
    }

    if (Array.isArray(tree.childFolders)) {
        for (const folder of tree.childFolders) {
            const result = findNodeById(folder, id, [...path]);
            if (result) {
                return result;
            }
        }
    }

    return null;
}
