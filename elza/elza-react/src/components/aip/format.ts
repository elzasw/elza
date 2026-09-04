/**
 * Size of the package in the units it is best read in, or "-" when it is not known - an AIP
 * whose package never arrived, or arrived broken, has no size to show.
 */
export const formatAipSize = (bytes: number | null | undefined): string => {
    if (bytes == null || !Number.isFinite(bytes) || bytes < 0) return '-';
    if (bytes === 0) return '0 B';

    const k = 1024;
    const sizes = ['B', 'kB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    const size = (bytes / Math.pow(k, i)).toFixed(1);

    return `${size} ${sizes[i]}`;
}
