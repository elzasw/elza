/**
 * Split a tree fullPath ("42/documents/2024") into (repoId, path):
 *   "42"                    → (42, undefined)
 *   "42/documents"          → (42, "documents")
 *   "42/documents/2024/x"   → (42, "documents/2024/x")
 *
 * The repoId is parsed as a number to match the elza-api endpoint signatures.
 */
export const extractRepoIdFromFullPath = (fullPath: string): [number, string | undefined] => {
    const firstSlashIndex = fullPath.indexOf("/");
    const repoIdStr = firstSlashIndex !== -1 ? fullPath.substring(0, firstSlashIndex) : fullPath;
    const path = firstSlashIndex !== -1 ? fullPath.substring(firstSlashIndex + 1) : undefined;
    return [parseInt(repoIdStr, 10), path];
}

