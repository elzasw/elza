import { Button, Checkbox, Combobox, Field, Input, Option } from '@fluentui/react-components';
import { DeleteRegular } from '@fluentui/react-icons';
import { useEffect, useState } from 'react';
import { Form, Field as FinalField } from 'react-final-form';
import { defineMessages, useIntl } from 'react-intl';
import { PublicationType } from 'elza-api';
import { WebApi } from 'actions/WebApi';
import { useDetailStyles } from './styles';
import { ConfirmPopover } from './ConfirmPopover';

const messages = defineMessages({
    sectionStorage:       { id: 'publication.system.section.storage',       defaultMessage: 'Ukládání exportů' },
    sectionPermissions:   { id: 'publication.system.section.permissions',   defaultMessage: 'Požadované oprávnění' },
    labelName:            { id: 'publication.system.label.name',            defaultMessage: 'Název' },
    labelCode:            { id: 'publication.system.label.code',            defaultMessage: 'Kód' },
    labelStoredCount:     { id: 'publication.system.label.storedCount',     defaultMessage: 'Počet uložených exportů' },
    labelFilter:          { id: 'publication.system.label.filter',          defaultMessage: 'Filtr' },
    labelPermExport:      { id: 'publication.system.label.permExport',      defaultMessage: 'Oprávnění pro export' },
    labelPermPublication: { id: 'publication.system.label.permPublication', defaultMessage: 'Oprávnění pro publikaci' },
    btnSave:              { id: 'publication.system.btn.save',              defaultMessage: 'Uložit' },
    btnReset:             { id: 'publication.system.btn.reset',             defaultMessage: 'Obnovit' },
    btnActivate:          { id: 'publication.system.btn.activate',          defaultMessage: 'Aktivovat' },
    btnDeactivate:        { id: 'publication.system.btn.deactivate',        defaultMessage: 'Deaktivovat' },
    confirmDeactivate:    { id: 'publication.system.btn.confirmDeactivate', defaultMessage: 'Opravdu deaktivovat tento typ?' },
    confirmRemove:        { id: 'publication.system.btn.confirmRemove',      defaultMessage: 'Opravdu odebrat tento typ?' },
    btnRemove:            { id: 'publication.system.btn.remove',             defaultMessage: 'Odebrat' },
});

interface ExportFilter {
    id?: number;
    code?: string;
    name?: string;
}

interface Props {
    value: PublicationType;
    onSave: (values: PublicationType) => void;
    onToggleActive: () => void;
    onRemove: () => void;
}

export type { Props as PublicationSystemDetailProps };

export function PublicationSystemDetail({ value, onSave, onToggleActive, onRemove }: Props) {
    const classes = useDetailStyles();
    const { formatMessage } = useIntl();

    const isInactive = !(value.active ?? true);

    const [exportFilters, setExportFilters] = useState<ExportFilter[]>([]);

    useEffect(() => {
        WebApi.findExportFilters().then((data: ExportFilter[]) => setExportFilters(data));
    }, []);

    return (
        <Form<PublicationType>
            initialValues={value}
            onSubmit={onSave}
            render={({ handleSubmit, form, dirty }) => (
                <div className={classes.root}>
                    <div className={classes.form}>
                        <div className={classes.section}>
                            <FinalField name="name" render={({ input }) => (
                                <Field label={formatMessage(messages.labelName)}>
                                    <Input disabled={isInactive} value={input.value} onChange={(_, data) => input.onChange(data.value)} />
                                </Field>
                            )} />
                            <FinalField name="code" render={({ input }) => (
                                <Field label={formatMessage(messages.labelCode)}>
                                    <Input disabled={isInactive} value={input.value} onChange={(_, data) => input.onChange(data.value)} />
                                </Field>
                            )} />
                        </div>

                        <div className={classes.section}>
                            <div className={classes.sectionTitle}>{formatMessage(messages.sectionStorage)}</div>
                            <FinalField name="retentionCount" render={({ input }) => (
                                <Field label={formatMessage(messages.labelStoredCount)}>
                                    <Input
                                        type="number"
                                        disabled={isInactive}
                                        value={String(input.value ?? 5)}
                                        onChange={(_, data) => {
                                            const parsed = parseInt(data.value, 10);
                                            input.onChange(isNaN(parsed) ? 0 : parsed);
                                        }}
                                    />
                                </Field>
                            )} />
                        </div>

                        <div className={classes.section}>
                            <FinalField name="exportFilterCode" render={({ input }) => {
                                const selectedFilter = exportFilters.find((f) => f.code === input.value);
                                return (
                                    <Field label={formatMessage(messages.labelFilter)}>
                                        <Combobox
                                            disabled={isInactive}
                                            placeholder="—"
                                            value={selectedFilter?.name ?? input.value ?? ''}
                                            selectedOptions={input.value ? [input.value] : []}
                                            onOptionSelect={(_, data) => input.onChange(data.optionValue ?? '')}
                                            onBlur={input.onBlur}
                                        >
                                            {exportFilters.map((filter) => (
                                                <Option key={filter.code} value={filter.code}>
                                                    {filter.name}
                                                </Option>
                                            ))}
                                        </Combobox>
                                    </Field>
                                );
                            }} />
                        </div>

                        <div className={classes.section}>
                            <div className={classes.sectionTitle}>{formatMessage(messages.sectionPermissions)}</div>
                            <FinalField name="allowPermExport" type="checkbox" render={({ input }) => (
                                <Checkbox
                                    disabled={isInactive}
                                    label={formatMessage(messages.labelPermExport)}
                                    checked={input.checked}
                                    onChange={(_, data) => input.onChange(!!data.checked)}
                                />
                            )} />
                            <FinalField name="allowPermPublication" type="checkbox" render={({ input }) => (
                                <Checkbox
                                    disabled={isInactive}
                                    label={formatMessage(messages.labelPermPublication)}
                                    checked={input.checked}
                                    onChange={(_, data) => input.onChange(!!data.checked)}
                                />
                            )} />
                        </div>
                    </div>

                    <div className={classes.footer}>
                        <Button appearance="primary" disabled={!dirty} onClick={handleSubmit}>
                            {formatMessage(messages.btnSave)}
                        </Button>
                        <Button disabled={!dirty} onClick={() => form.reset()}>
                            {formatMessage(messages.btnReset)}
                        </Button>
                        <div style={{ flex: 1 }} />
                        {isInactive
                            ? <Button onClick={onToggleActive}>
                                {formatMessage(messages.btnActivate)}
                              </Button>
                            : <ConfirmPopover
                                text={formatMessage(messages.confirmDeactivate)}
                                confirmLabel={formatMessage(messages.btnDeactivate)}
                                positioning="above"
                                onConfirm={onToggleActive}
                              >
                                <Button className={classes.dangerBtn}>{formatMessage(messages.btnDeactivate)}</Button>
                              </ConfirmPopover>
                        }
                        <ConfirmPopover
                            text={formatMessage(messages.confirmRemove)}
                            confirmLabel={formatMessage(messages.btnRemove)}
                            positioning="above"
                            onConfirm={onRemove}
                        >
                            <Button icon={<DeleteRegular />} className={classes.dangerBtn} />
                        </ConfirmPopover>
                    </div>
                </div>
            )}
        />
    );
}
