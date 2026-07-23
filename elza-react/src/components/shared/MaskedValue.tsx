import { useState } from 'react';
import { Button, makeStyles, tokens } from '@fluentui/react-components';
import { Eye16Regular, EyeOff16Regular } from '@fluentui/react-icons';
import { defineMessages, useIntl } from 'react-intl';

interface Props {
    value?: string | null;
    className?: string;
}

export type MaskedValueProps = Props;

const useStyles = makeStyles({
    root: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalXS,
        maxWidth: '100%',
    },
    value: {
        fontFamily: tokens.fontFamilyMonospace,
        wordBreak: 'break-all',
        minWidth: 0,
    },
    toggle: {
        minWidth: 'auto',
        padding: '0',
    },
});

const MASK = '••••••••';

export function MaskedValue({ value, className }: Props) {
    const [revealed, setRevealed] = useState(false);
    const styles = useStyles();
    const intl = useIntl();

    const hasValue = value != null && value !== '';
    const toggleLabel = intl.formatMessage(revealed ? messages.hide : messages.show);

    return (
        <span className={`${styles.root} ${className ?? ''}`}>
            <span className={styles.value}>{hasValue ? (revealed ? value : MASK) : ''}</span>
            {hasValue && (
                <Button
                    className={styles.toggle}
                    appearance="subtle"
                    size="small"
                    aria-label={toggleLabel}
                    title={toggleLabel}
                    icon={revealed ? <EyeOff16Regular /> : <Eye16Regular />}
                    onClick={() => setRevealed((prev) => !prev)}
                />
            )}
        </span>
    );
}

const messages = defineMessages({
    show: {
        id: 'shared.maskedValue.show',
        defaultMessage: 'Zobrazit',
    },
    hide: {
        id: 'shared.maskedValue.hide',
        defaultMessage: 'Skrýt',
    },
});
