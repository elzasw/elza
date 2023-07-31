export const extractRepoIdFromFullPath = (fullPath: string): [string, string | undefined] => {
    const firstSlashIndex = fullPath.indexOf("/");
    if (firstSlashIndex != -1) {
        return [fullPath.substring(0, firstSlashIndex), fullPath.substring(firstSlashIndex + 1)];
    }
    else {
        return [fullPath.substring(0), undefined];
    }
}

