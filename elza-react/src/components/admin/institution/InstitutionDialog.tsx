import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Dropdown,
    Field,
    Input,
    Option,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { Institution, InstitutionType } from 'elza-api';
import { useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { AccessPointPicker } from './AccessPointPicker';

const messages = defineMessages({
    createTitle: {
        id: 'admin.institution.dialog.createTitle',
        defaultMessage: 'Nová instituce',
    },
    editTitle: {
        id: 'admin.institution.dialog.editTitle',
        defaultMessage: 'Úprava instituce',
    },
    internalCode: {
        id: 'admin.institution.field.internalCode',
        defaultMessage: 'Interní kód',
    },
    accessPoint: {
        id: 'admin.institution.field.accessPoint',
        defaultMessage: 'Archivní entita',
    },
    type: {
        id: 'admin.institution.field.type',
        defaultMessage: 'Typ instituce',
    },
    shortName: {
        id: 'admin.institution.field.shortName',
        defaultMessage: 'Zkrácený název',
    },
    name: {
        id: 'admin.institution.field.name',
        defaultMessage: 'Název',
    },
    typePlaceholder: {
        id: 'admin.institution.field.typePlaceholder',
        defaultMessage: 'Vyberte typ',
    },
    delete: {
        id: 'admin.institution.action.delete',
        defaultMessage: 'Smazat',
    },
    save: {
        id: 'admin.institution.action.save',
        defaultMessage: 'Uložit',
    },
});

const useStyles = makeStyles({
    body: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
    actions: {
        justifyContent: 'space-between',
    },
    rightActions: {
        display: 'flex',
        columnGap: tokens.spacingHorizontalS,
    },
    dangerButton: {
        color: tokens.colorStatusDangerForeground1,
        borderTopColor: tokens.colorStatusDangerBorder1,
        borderRightColor: tokens.colorStatusDangerBorder1,
        borderBottomColor: tokens.colorStatusDangerBorder1,
        borderLeftColor: tokens.colorStatusDangerBorder1,
        ':hover': {
            color: tokens.colorStatusDangerForeground1,
            borderTopColor: tokens.colorStatusDangerBorder1,
            borderRightColor: tokens.colorStatusDangerBorder1,
            borderBottomColor: tokens.colorStatusDangerBorder1,
            borderLeftColor: tokens.colorStatusDangerBorder1,
            backgroundColor: tokens.colorStatusDangerBackground1,
        },
        ':hover:active': {
            color: tokens.colorStatusDangerForeground1,
            borderTopColor: tokens.colorStatusDangerBorder1,
            borderRightColor: tokens.colorStatusDangerBorder1,
            borderBottomColor: tokens.colorStatusDangerBorder1,
            borderLeftColor: tokens.colorStatusDangerBorder1,
            backgroundColor: tokens.colorStatusDangerBackground2,
        },
    },
});

interface Props {
    open: boolean;
    institution?: Institution;
    types: InstitutionType[];
    onSubmit: (values: Institution) => Promise<void>;
    onDelete?: () => Promise<void>;
    onClose: () => void;
}

export function InstitutionDialog({ open, institution, types, onSubmit, onDelete, onClose }: Props) {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const isEdit = institution != null;

    const [internalCode, setInternalCode] = useState('');
    const [shortName, setShortName] = useState('');
    const [name, setName] = useState('');
    const [institutionTypeId, setInstitutionTypeId] = useState<number>();
    const [accessPointId, setAccessPointId] = useState<number>();
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        if (!open) {
            return;
        }
        setInternalCode(institution?.internalCode ?? '');
        setShortName(institution?.shortName ?? '');
        setName(institution?.name ?? '');
        setInstitutionTypeId(institution?.institutionTypeId);
        setAccessPointId(institution?.accessPointId);
        setIsSaving(false);
    }, [open, institution]);

    const isValid = internalCode.trim().length > 0 && (isEdit || accessPointId != null);

    const handleSubmit = async () => {
        if (!isValid) {
            return;
        }
        setIsSaving(true);
        try {
            await onSubmit({
                id: institution?.id ?? 0,
                internalCode: internalCode.trim(),
                accessPointId: accessPointId!,
                institutionTypeId,
                shortName: shortName || undefined,
                name: name || undefined,
            });
        } finally {
            setIsSaving(false);
        }
    };

    const selectedType = types.find(type => type.id === institutionTypeId);

    return (
        <Dialog open={open} onOpenChange={(_event, data) => !data.open && onClose()}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>
                        <FormattedMessage {...(isEdit ? messages.editTitle : messages.createTitle)} />
                    </DialogTitle>
                    <DialogContent className={styles.body}>
                        <Field label={formatMessage(messages.internalCode)} required>
                            <Input
                                value={internalCode}
                                onChange={(_event, data) => setInternalCode(data.value)}
                            />
                        </Field>
                        <Field label={formatMessage(messages.accessPoint)} required={!isEdit}>
                            <AccessPointPicker
                                value={accessPointId}
                                onChange={setAccessPointId}
                                disabled={isEdit}
                            />
                        </Field>
                        <Field label={formatMessage(messages.type)}>
                            <Dropdown
                                placeholder={formatMessage(messages.typePlaceholder)}
                                selectedOptions={institutionTypeId != null ? [institutionTypeId.toString()] : []}
                                value={selectedType?.name ?? ''}
                                onOptionSelect={(_event, data) =>
                                    setInstitutionTypeId(
                                        data.optionValue != null ? parseInt(data.optionValue) : undefined,
                                    )
                                }
                            >
                                {types.map(type => (
                                    <Option key={type.id} value={type.id.toString()} text={type.name}>
                                        {type.name}
                                    </Option>
                                ))}
                            </Dropdown>
                        </Field>
                        <Field label={formatMessage(messages.shortName)}>
                            <Input value={shortName} onChange={(_event, data) => setShortName(data.value)} />
                        </Field>
                        {isEdit && (
                            <Field label={formatMessage(messages.name)}>
                                <Input value={name} onChange={(_event, data) => setName(data.value)} />
                            </Field>
                        )}
                    </DialogContent>
                    <DialogActions className={styles.actions}>
                        <div>
                            {isEdit && onDelete && (
                                <Button
                                    appearance="outline"
                                    className={styles.dangerButton}
                                    disabled={isSaving}
                                    onClick={onDelete}
                                >
                                    <FormattedMessage {...messages.delete} />
                                </Button>
                            )}
                        </div>
                        <div className={styles.rightActions}>
                            <Button appearance="secondary" disabled={isSaving} onClick={onClose}>
                                <FormattedMessage {...globalMessages.cancel} />
                            </Button>
                            <Button
                                appearance="primary"
                                disabled={!isValid || isSaving}
                                onClick={handleSubmit}
                            >
                                <FormattedMessage {...messages.save} />
                            </Button>
                        </div>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}

export type InstitutionDialogProps = Props;
