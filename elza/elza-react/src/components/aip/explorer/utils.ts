export const getFileName = (name: string): string => {
    return name ? name.substring(name.lastIndexOf("/") + 1) : "-";
}

export const turncate = (str: string ): string => {
    if (str.length <= 20) {
        return str;
    }
    return str.slice(0, 17) + '...';
}

export const findNodeByUUID = (tree, uuid, path = []) => {
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

export const findNodeById = (tree, id, path = []) => {
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
