import { FormattedMessage } from 'react-intl';

import { icuValues } from 'components/shared/lang/values';
import { exceptionMessages, hasExceptionMessage } from './messages';
import { exceptionMessageId, type ExceptionPayload } from './exceptionKey';

/**
 * Titulek chybové hlášky ze serveru.
 *
 * Formátuje se až při renderu, ne při vzniku chyby: toastr tak text přepíše
 * i při přepnutí jazyka. Neznámý kód se nevypisuje jako `[klíč]` - zobrazí se
 * technická zpráva ze serveru a do konzole jde upozornění, aby se hláška
 * doplnila.
 */
export function ExceptionTitle({ data }: { data: ExceptionPayload }) {
    const id = exceptionMessageId(data);

    if (!id || !hasExceptionMessage(id)) {
        if (process.env.NODE_ENV !== 'production') {
            console.warn(
                `i18n: chybí hláška '${id ?? data.code}', doplň ji do components/exception/messages.ts`,
            );
        }
        return <>{data.message ?? data.code ?? ''}</>;
    }

    return <FormattedMessage {...exceptionMessages[id]} values={icuValues(data.properties)} />;
}
