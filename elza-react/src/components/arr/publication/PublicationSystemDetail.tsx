import { Button, Checkbox, Combobox, Field, Input } from '@fluentui/react-components';
import { defineMessages, useIntl } from 'react-intl';
import { PublicationType } from 'elza-api';
import { useDetailStyles } from './styles';

const messages = defineMessages({
    sectionStorage: { id: 'publication.system.section.storage', defaultMessage: 'Ukládání exportů' },
    sectionPermissions: { id: 'publication.system.section.permissions', defaultMessage: 'Požadované oprávnění' },
    labelName: { id: 'publication.system.label.name', defaultMessage: 'Název' },
    labelCode: { id: 'publication.system.label.code', defaultMessage: 'Kód' },
    labelStoredCount: { id: 'publication.system.label.storedCount', defaultMessage: 'Počet uložených exportů' },
    labelFilter: { id: 'publication.system.label.filter', defaultMessage: 'Filtr' },
    labelPermExport: { id: 'publication.system.label.permExport', defaultMessage: 'Oprávnění pro export' },
    labelPermPublication: { id: 'publication.system.label.permPublication', defaultMessage: 'Oprávnění pro publikaci' },
    btnSave: { id: 'publication.system.btn.save', defaultMessage: 'Uložit' },
    btnReset: { id: 'publication.system.btn.reset', defaultMessage: 'Obnovit' },
});

interface Props {
    value: PublicationType;
    onChange: (updated: PublicationType) => void;
    onSave: () => void;
    onReset: () => void;
}

export type { Props as PublicationSystemDetailProps };

export function PublicationSystemDetail({ value, onChange, onSave, onReset }: Props) {
    const classes = useDetailStyles();
    const { formatMessage } = useIntl();

    const update = (patch: Partial<PublicationType>) => onChange({ ...value, ...patch });
    const isInactive = !(value.active ?? true);

    return (
        <div className={classes.root}>
            <div className={classes.form}>
                <div className={classes.section}>
                    <Field label={formatMessage(messages.labelName)}>
                        <Input disabled={isInactive} value={value.name} onChange={(_, data) => update({ name: data.value })} />
                    </Field>
                    <Field label={formatMessage(messages.labelCode)}>
                        <Input disabled={isInactive} value={value.code} onChange={(_, data) => update({ code: data.value })} />
                    </Field>
                </div>

                <div className={classes.section}>
                    <div className={classes.sectionTitle}>{formatMessage(messages.sectionStorage)}</div>
                    <Field label={formatMessage(messages.labelStoredCount)}>
                        <Input
                            type="number"
                            disabled={isInactive}
                            value={String(value.retentionCount ?? 5)}
                            min={0}
                            onChange={(_, data) => {
                                const parsed = parseInt(data.value, 10);
                                update({ retentionCount: isNaN(parsed) ? 0 : parsed });
                            }}
                        />
                    </Field>
                </div>

                <div className={classes.section}>
                    <Field label={formatMessage(messages.labelFilter)}>
                        <Combobox value={value.exportFilterCode ?? ''} placeholder="—" disabled />
                    </Field>
                </div>

                <div className={classes.section}>
                    <div className={classes.sectionTitle}>{formatMessage(messages.sectionPermissions)}</div>
                    <Checkbox
                        disabled={isInactive}
                        label={formatMessage(messages.labelPermExport)}
                        checked={value.allowPermExport}
                        onChange={(_, data) => update({ allowPermExport: !!data.checked })}
                    />
                    <Checkbox
                        disabled={isInactive}
                        label={formatMessage(messages.labelPermPublication)}
                        checked={value.allowPermPublication}
                        onChange={(_, data) => update({ allowPermPublication: !!data.checked })}
                    />
                </div>
            </div>

            <div className={classes.footer}>
                <Button appearance="primary" onClick={onSave}>
                    {formatMessage(messages.btnSave)}
                </Button>
                <Button onClick={onReset}>{formatMessage(messages.btnReset)}</Button>
            </div>
        </div>
    );
}
