import {
    Button,
    Dropdown,
    Field as FluentField,
    Input,
    Option,
    Title3,
    Toolbar,
    ToolbarButton,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { ArrowLeftRegular, ChevronRightRegular } from '@fluentui/react-icons';
import { Institution, InstitutionType } from 'elza-api';
import { Field, Form } from 'react-final-form';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { AccessPointPicker } from 'components/registry';

const messages = defineMessages({
    createTitle: {
        id: 'admin.institution.detail.createTitle',
        defaultMessage: 'Nová instituce',
    },
    back: {
        id: 'admin.institution.detail.back',
        defaultMessage: 'Zpět',
    },
    overline: {
        id: 'admin.institution.detail.overline',
        defaultMessage: 'Instituce',
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
    root: {
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        rowGap: tokens.spacingVerticalM,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalS,
    },
    crumb: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalXS,
        minWidth: 0,
    },
    crumbRoot: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase300,
    },
    crumbSeparator: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase300,
        display: 'flex',
    },
    title: {
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
        maxWidth: '480px',
        padding: `${tokens.spacingVerticalL} ${tokens.spacingHorizontalXXL}`,
    },
    actions: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalS,
        marginTop: tokens.spacingVerticalM,
    },
    spacer: {
        flexGrow: 1,
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

interface FormValues {
    internalCode: string;
    accessPointId?: number;
    institutionTypeId?: number;
    shortName?: string;
    name?: string;
}

interface Props {
    institution?: Institution;
    types: InstitutionType[];
    canEdit: boolean;
    onSubmit: (values: Institution) => Promise<void>;
    onDelete?: () => Promise<void>;
    onBack: () => void;
}

export function InstitutionDetail({ institution, types, canEdit, onSubmit, onDelete, onBack }: Props) {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const isEdit = institution != null;

    const initialValues: FormValues = {
        internalCode: institution?.internalCode ?? '',
        accessPointId: institution?.accessPointId,
        institutionTypeId: institution?.institutionTypeId,
        shortName: institution?.shortName ?? '',
        name: institution?.name ?? '',
    };

    const validate = (values: FormValues) => {
        const errors: Partial<Record<keyof FormValues, string>> = {};
        if (!values.name?.trim()) {
            errors.name = formatMessage(messages.name);
        }
        if (!values.internalCode?.trim()) {
            errors.internalCode = formatMessage(messages.internalCode);
        }
        if (values.accessPointId == null) {
            errors.accessPointId = formatMessage(messages.accessPoint);
        }
        return errors;
    };

    const handleSubmit = async (values: FormValues) => {
        await onSubmit({
            id: institution?.id ?? 0,
            internalCode: values.internalCode.trim(),
            accessPointId: values.accessPointId!,
            institutionTypeId: values.institutionTypeId,
            shortName: values.shortName || undefined,
            name: values.name || undefined,
        });
    };

    const title = isEdit
        ? institution.name || institution.shortName || institution.internalCode
        : formatMessage(messages.createTitle);

    return (
        <div className={styles.root}>
            <Toolbar className={styles.header}>
                <ToolbarButton
                    icon={<ArrowLeftRegular />}
                    onClick={onBack}
                    aria-label={formatMessage(messages.back)}
                />
                <div className={styles.crumb}>
                    <span className={styles.crumbRoot}>
                        <FormattedMessage {...messages.overline} />
                    </span>
                    <ChevronRightRegular className={styles.crumbSeparator} />
                    <Title3 className={styles.title}>{title}</Title3>
                </div>
            </Toolbar>
            <Form<FormValues> onSubmit={handleSubmit} validate={validate} initialValues={initialValues}>
                {({ handleSubmit, submitting, pristine, invalid }) => (
                    <form className={styles.form} onSubmit={handleSubmit}>
                        <Field<string> name="name">
                            {({ input }) => (
                                <FluentField label={formatMessage(messages.name)} required>
                                    <Input
                                        value={input.value}
                                        disabled={!canEdit}
                                        onChange={(_event, data) => input.onChange(data.value)}
                                    />
                                </FluentField>
                            )}
                        </Field>
                        <Field<string> name="shortName">
                            {({ input }) => (
                                <FluentField label={formatMessage(messages.shortName)}>
                                    <Input
                                        value={input.value}
                                        disabled={!canEdit}
                                        onChange={(_event, data) => input.onChange(data.value)}
                                    />
                                </FluentField>
                            )}
                        </Field>
                        <Field<string> name="internalCode">
                            {({ input }) => (
                                <FluentField label={formatMessage(messages.internalCode)} required>
                                    <Input
                                        value={input.value}
                                        disabled={!canEdit}
                                        onChange={(_event, data) => input.onChange(data.value)}
                                    />
                                </FluentField>
                            )}
                        </Field>
                        <Field<number | undefined> name="accessPointId">
                            {({ input }) => (
                                <FluentField label={formatMessage(messages.accessPoint)} required>
                                    <AccessPointPicker
                                        value={input.value}
                                        onChange={input.onChange}
                                        disabled={!canEdit}
                                    />
                                </FluentField>
                            )}
                        </Field>
                        <Field<number | undefined> name="institutionTypeId">
                            {({ input }) => {
                                const selectedType = types.find(type => type.id === input.value);
                                return (
                                    <FluentField label={formatMessage(messages.type)}>
                                        <Dropdown
                                            placeholder={formatMessage(messages.typePlaceholder)}
                                            disabled={!canEdit}
                                            selectedOptions={input.value != null ? [input.value.toString()] : []}
                                            value={selectedType?.name ?? ''}
                                            onOptionSelect={(_event, data) =>
                                                input.onChange(
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
                                    </FluentField>
                                );
                            }}
                        </Field>
                        {canEdit && (
                            <div className={styles.actions}>
                                <Button
                                    type="submit"
                                    appearance="primary"
                                    disabled={pristine || invalid || submitting}
                                >
                                    <FormattedMessage {...messages.save} />
                                </Button>
                                <div className={styles.spacer} />
                                {isEdit && onDelete && (
                                    <Button
                                        type="button"
                                        appearance="outline"
                                        className={styles.dangerButton}
                                        disabled={submitting}
                                        onClick={onDelete}
                                    >
                                        <FormattedMessage {...messages.delete} />
                                    </Button>
                                )}
                            </div>
                        )}
                    </form>
                )}
            </Form>
        </div>
    );
}

export type InstitutionDetailProps = Props;
