import {
    Button,
    Field as FluentField,
    Select,
    Textarea,
    makeStyles,
    mergeClasses,
    tokens,
} from '@fluentui/react-components';
import * as issuesActions from 'actions/arr/issues';
import * as issueTypesActions from 'actions/refTables/issueTypes';
import { globalMessages } from 'components/shared/lang/messages';
import { useEffect } from 'react';
import { Field, Form } from 'react-final-form';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { IssueListVO, IssueVO } from 'types';
import { useActiveFund, useAppThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    type: { id: 'issue.type', defaultMessage: 'Typ připomínky' },
    issueList: { id: 'issue.issueList', defaultMessage: 'Protokol' },
    text: { id: 'issue.text', defaultMessage: 'Text připomínky' },
});

const useStyles = makeStyles({
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
        padding: `${tokens.spacingVerticalL} ${tokens.spacingHorizontalL}`,
    },
    actions: {
        display: 'flex',
        justifyContent: 'flex-end',
        gap: tokens.spacingHorizontalS,
    },
});

export interface IssueFormValues {
    issueListId?: number;
    issueTypeId?: number;
    description?: string;
}

interface IssueProtocolsStore {
    fetched: boolean;
    count: number;
    rows: IssueListVO[];
}

interface Props {
    initialValues: IssueFormValues;
    onSubmit: (values: IssueFormValues) => Promise<IssueVO>;
    onSubmitSuccess?: (issue: IssueVO) => void;
    /** Injected by `modalDialogShow`. */
    onClose?: () => void;
    update?: boolean;
    className?: string;
}

function toOptionalId(value: string) {
    return value ? Number(value) : undefined;
}

export function IssueForm({ initialValues, onSubmit, onSubmitSuccess, onClose, update = false, className }: Props) {
    const intl = useIntl();
    const styles = useStyles();
    const dispatch = useAppThunkDispatch();
    const activeFundId = useActiveFund()?.id;
    const issueTypes = useAppSelector(({ refTables }) => refTables.issueTypes);
    const issueProtocols = useAppSelector(({ app }) => app.issueProtocols as IssueProtocolsStore);

    useEffect(() => {
        dispatch(issueTypesActions.fetchIfNeeded());
    }, [dispatch]);

    useEffect(() => {
        if (activeFundId != null) {
            dispatch(issuesActions.protocols.fetchIfNeeded(activeFundId));
        }
    }, [dispatch, activeFundId]);

    function validate(values: IssueFormValues) {
        const errors: Partial<Record<keyof IssueFormValues, string>> = {};
        const requiredMessage = intl.formatMessage(globalMessages.validationRequired);

        if (!values.issueTypeId) {
            errors.issueTypeId = requiredMessage;
        }
        if (!values.description) {
            errors.description = requiredMessage;
        }

        return errors;
    }

    async function handleSubmit(values: IssueFormValues) {
        const savedIssue = await onSubmit(values);
        onSubmitSuccess?.(savedIssue);
    }

    const hasNoProtocols = issueProtocols.fetched && issueProtocols.count === 0;
    const protocolOptions = issueProtocols.fetched ? issueProtocols.rows : [];
    const issueTypeOptions = issueTypes.fetched ? (issueTypes.data ?? []) : [];

    return (
        <Form<IssueFormValues> initialValues={initialValues} validate={validate} onSubmit={handleSubmit}>
            {({ handleSubmit: submitForm, submitting }) => (
                // noValidate: Fluent `required` sets the native attribute, whose browser check would block
                // the submit before final-form validation shows its own messages
                <form className={mergeClasses(styles.form, className)} onSubmit={submitForm} noValidate>
                    <Field<number> name="issueListId">
                        {({ input }) => (
                            <FluentField label={intl.formatMessage(messages.issueList)}>
                                <Select
                                    value={input.value ? String(input.value) : ''}
                                    onChange={(_event, data) => input.onChange(toOptionalId(data.value))}
                                >
                                    {hasNoProtocols && <option value="" />}
                                    {protocolOptions.map(({ id, name }) => (
                                        <option key={id} value={id}>
                                            {name}
                                        </option>
                                    ))}
                                </Select>
                            </FluentField>
                        )}
                    </Field>
                    <Field<number> name="issueTypeId">
                        {({ input, meta }) => {
                            const errorMessage = meta.touched ? meta.error : undefined;
                            return (
                                <FluentField
                                    label={intl.formatMessage(messages.type)}
                                    required
                                    validationMessage={errorMessage}
                                    validationState={errorMessage ? 'error' : 'none'}
                                >
                                    <Select
                                        value={input.value ? String(input.value) : ''}
                                        onChange={(_event, data) => input.onChange(toOptionalId(data.value))}
                                        onBlur={input.onBlur}
                                    >
                                        {issueTypeOptions.map(({ id, name }) => (
                                            <option key={id} value={id}>
                                                {name}
                                            </option>
                                        ))}
                                    </Select>
                                </FluentField>
                            );
                        }}
                    </Field>
                    <Field<string> name="description">
                        {({ input, meta }) => {
                            const errorMessage = meta.touched ? meta.error : undefined;
                            return (
                                <FluentField
                                    label={intl.formatMessage(messages.text)}
                                    required
                                    validationMessage={errorMessage}
                                    validationState={errorMessage ? 'error' : 'none'}
                                >
                                    <Textarea
                                        resize="vertical"
                                        rows={5}
                                        value={input.value ?? ''}
                                        onChange={(_event, data) => input.onChange(data.value)}
                                        onBlur={input.onBlur}
                                    />
                                </FluentField>
                            );
                        }}
                    </Field>
                    <div className={styles.actions}>
                        <Button type="submit" appearance="primary" disabled={submitting}>
                            <FormattedMessage {...(update ? globalMessages.save : globalMessages.add)} />
                        </Button>
                        <Button onClick={onClose} disabled={submitting}>
                            <FormattedMessage {...globalMessages.cancel} />
                        </Button>
                    </div>
                </form>
            )}
        </Form>
    );
}

export type IssueFormProps = Props;
