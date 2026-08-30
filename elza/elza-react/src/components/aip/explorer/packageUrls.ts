import { serverContextPath } from '../../../api';

/** Obsah jednoho souboru staženého balíčku. */
export const packageEntryUrl = (aipId: number, path: string) =>
    `${serverContextPath}/api/v1/aip/${aipId}/package/content?path=${encodeURIComponent(path)}`;

/** Celý stažený balíček tak, jak přišel z digitálního archivu. */
export const packageDownloadUrl = (aipId: number) =>
    `${serverContextPath}/api/v1/aip/${aipId}/package/download`;
