import {
    Combobox,
    Option,
    OptionOnSelectData,
    SelectionEvents,
    Spinner,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { WebApi } from 'actions';
import { ApAccessPointVO } from 'api';
import { useEffect, useState } from 'react';
import { FormattedMessage, defineMessages } from 'react-intl';
import { useDebouncedEffect } from 'utils/hooks/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';

const messages = defineMessages({
    placeholder: {
        id: 'admin.institution.ap.placeholder',
        defaultMessage: 'Vyhledat archivní entitu',
    },
    noResults: {
        id: 'admin.institution.ap.noResults',
        defaultMessage: 'Žádné výsledky',
    },
    startTyping: {
        id: 'admin.institution.ap.startTyping',
        defaultMessage: 'Začněte psát pro vyhledávání',
    },
});

const useStyles = makeStyles({
    combobox: {
        width: '100%',
    },
    listbox: {
        maxHeight: '400px',
    },
    option: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        rowGap: tokens.spacingVerticalXXS,
    },
    optionMeta: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase200,
    },
    status: {
        padding: tokens.spacingVerticalS,
        color: tokens.colorNeutralForeground3,
    },
});

interface Props {
    value?: number;
    onChange: (accessPointId: number | undefined) => void;
    disabled?: boolean;
    clearable?: boolean;
}

export function AccessPointPicker({ value, onChange, disabled, clearable }: Props) {
    const styles = useStyles();
    const apTypesMap = useAppSelector(({ refTables }) => refTables.recordTypes.itemsMap);

    const [query, setQuery] = useState('');
    const [accessPoints, setAccessPoints] = useState<ApAccessPointVO[]>([]);
    const [isLoading, setIsLoading] = useState(false);

    const hasAccessPointId = typeof value === 'number' && !Number.isNaN(value);

    useEffect(() => {
        if (!hasAccessPointId) {
            setQuery('');
            return;
        }
        (async () => {
            const accessPoint = await WebApi.getAccessPoint(value);
            setQuery(accessPoint.name);
        })();
    }, [value]);

    useDebouncedEffect(
        () => {
            const hasEnoughCharacters = query.length >= 1;
            if (!hasEnoughCharacters) {
                setAccessPoints([]);
                setIsLoading(false);
                return;
            }
            (async () => {
                setAccessPoints([]);
                setIsLoading(true);
                try {
                    const result = await WebApi.findAccessPoint(query);
                    setAccessPoints(result.rows);
                } finally {
                    setIsLoading(false);
                }
            })();
        },
        500,
        [query],
    );

    const handleSelect = (_event: SelectionEvents, data: OptionOnSelectData) => {
        if (!data.optionValue) {
            if (data.selectedOptions.length === 0) {
                setQuery('');
                setAccessPoints([]);
                onChange(undefined);
            }
            return;
        }
        setQuery(data.optionText ?? '');
        onChange(parseInt(data.optionValue));
    };

    const hasQuery = query.length >= 1;

    return (
        <Combobox
            className={styles.combobox}
            placeholder={messages.placeholder.defaultMessage}
            clearable={clearable}
            value={query}
            selectedOptions={hasAccessPointId ? [value.toString()] : []}
            onChange={event => {
                setQuery(event.target.value);
                setAccessPoints([]);
                setIsLoading(event.target.value.length >= 1);
            }}
            onOptionSelect={handleSelect}
            listbox={{ className: styles.listbox }}
            disabled={disabled}
        >
            {accessPoints.map(({ id, name, description, typeId }) => {
                const typeName = (apTypesMap as Record<number, { name: string }>)?.[typeId]?.name;
                const meta = [typeName, description].filter(Boolean).join(' · ');
                return (
                    <Option key={id} text={name} value={id.toString()}>
                        <span className={styles.option}>
                            <span>{name}</span>
                            {meta && <span className={styles.optionMeta}>{meta}</span>}
                        </span>
                    </Option>
                );
            })}
            {isLoading && (
                <div className={styles.status}>
                    <Spinner size="tiny" />
                </div>
            )}
            {!isLoading && accessPoints.length === 0 && (
                <div className={styles.status}>
                    {hasQuery ? (
                        <FormattedMessage {...messages.noResults} />
                    ) : (
                        <FormattedMessage {...messages.startTyping} />
                    )}
                </div>
            )}
        </Combobox>
    );
}

export type AccessPointPickerProps = Props;
