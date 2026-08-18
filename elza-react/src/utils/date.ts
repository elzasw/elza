import { pad2 } from 'components/validate';

/**
 * Datum ve tvaru DD.MM.YYYY.
 *
 * Pozor na `formatDate` v components/validate, které stejný vstup zformátuje jako
 * YYYY-MM-DD. Nové kódy raději používejte `intl.formatDate` z react-intl.
 */
export const formatDateCz = (date: Date): string => {
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const year = date.getFullYear();
    return [pad2(day), pad2(month), year].join('.');
}
