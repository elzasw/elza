import { IntlShape } from 'react-intl';
import { messages } from './messages';

/**
 * When a request is taken again, relative to now: "teď" when it is due, otherwise in the
 * largest unit that keeps the number readable.
 */
export function formatNextAttempt(intl: IntlShape, nextAttemptAt: string | undefined, now: number = Date.now()): string {
    if (nextAttemptAt == null) {
        return intl.formatMessage(messages.now);
    }
    const seconds = Math.round((new Date(nextAttemptAt).getTime() - now) / 1000);
    if (seconds <= 0) {
        return intl.formatMessage(messages.now);
    }
    if (seconds < 90) {
        return intl.formatRelativeTime(seconds, 'second');
    }
    if (seconds < 90 * 60) {
        return intl.formatRelativeTime(Math.round(seconds / 60), 'minute');
    }
    return intl.formatRelativeTime(Math.round(seconds / 3600), 'hour');
}

export function formatDateTime(intl: IntlShape, value: string | undefined): string {
    return value == null ? '' : intl.formatDate(value, { dateStyle: 'short', timeStyle: 'medium' });
}
