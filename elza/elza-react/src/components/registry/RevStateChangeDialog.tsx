import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Dropdown,
    Field as FluentField,
    MessageBar,
    MessageBarBody,
    MessageBarTitle,
    Option,
    Textarea,
} from '@fluentui/react-components';
import { Dismiss24Regular } from '@fluentui/react-icons';
import { globalMessages } from 'components/shared/lang/messages';
import { RevStateChange } from 'elza-api';
import { Field, Form } from 'react-final-form';
import { FormattedMessage, useIntl } from 'react-intl';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { RevStateApproval, RevStateApprovalCaption } from '../../api/RevStateApproval';
import { stateChangeMessages as messages, useStateChangeDialogStyles } from './stateChangeShared';
import { ApTypePicker } from './ApTypePicker';
import { AssignedUserField } from './AssignedUserField';

export interface RevStateFormFields extends RevStateChange {
    assignedTo?: number;
}

export interface RevStateChangeDialogProps {
    title: string;
    accessPointId: number;
    /** Scope of the entity: its rule set decides the offered classes. */
    scopeId?: number;
    initialValues: Partial<RevStateFormFields>;
    onClose: () => void;
    onSubmit: (data: RevStateFormFields) => Promise<unknown> | unknown;
}

/**
 * Change of the state, class and assignment of a revision. A revision with validation errors cannot
 * be sent for approval; the errors are listed.
 */
export const RevStateChangeDialog = ({
    title,
    accessPointId,
    scopeId,
    initialValues,
    onClose,
    onSubmit,
}: RevStateChangeDialogProps) => {
    const intl = useIntl();
    const styles = useStateChangeDialogStyles();
    const { data: validationData } = useAppSelector(({ app }) => app.apValidation);
    const { id: currentUserId } = useAppSelector(({ userDetail }) => userDetail);

    const errors = [
        ...(validationData?.errors ?? []),
        ...(validationData?.partErrors ?? []).flatMap((partError) => partError?.errors ?? []),
    ];
    const isValid = errors.length === 0;

    const states = [RevStateApproval.ACTIVE, RevStateApproval.TO_AMEND];
    if (isValid) {
        states.push(RevStateApproval.TO_APPROVE);
    }

    const validate = (values: RevStateFormFields) => {
        const result: Partial<Record<keyof RevStateFormFields, string>> = {};
        if (!values.state) {
            result.state = intl.formatMessage(globalMessages.validationRequired);
        }
        if (values.state === RevStateApproval.TO_APPROVE && values.assignedTo === currentUserId) {
            result.assignedTo = intl.formatMessage(messages.toApproveSameUser);
        }
        return result;
    };

    return (
        <Dialog open modalType="modal" onOpenChange={(_event, data) => !data.open && onClose()}>
            <DialogSurface className={styles.surface}>
                <Form<RevStateFormFields> initialValues={initialValues} onSubmit={onSubmit} validate={validate}>
                    {({ submitting, handleSubmit, values, valid }) => (
                        <form onSubmit={handleSubmit}>
                            <DialogBody>
                                <DialogTitle
                                    action={
                                        <Button
                                            appearance="subtle"
                                            aria-label={intl.formatMessage(globalMessages.close)}
                                            icon={<Dismiss24Regular />}
                                            onClick={onClose}
                                        />
                                    }
                                >
                                    {title}
                                </DialogTitle>
                                <DialogContent className={styles.content}>
                                    {!isValid && (
                                        <MessageBar intent="error" layout="multiline">
                                            <MessageBarBody>
                                                <MessageBarTitle>
                                                    {intl.formatMessage(messages.validationErrors)}
                                                </MessageBarTitle>
                                                <ul>
                                                    {errors.map((error, index) => (
                                                        <li key={index}>{error}</li>
                                                    ))}
                                                </ul>
                                            </MessageBarBody>
                                        </MessageBar>
                                    )}
                                    <Field<number> name="typeId">
                                        {({ input }) => (
                                            <ApTypePicker
                                                label={intl.formatMessage(messages.type)}
                                                scopeId={scopeId}
                                                value={input.value || undefined}
                                                onChange={(type) => input.onChange(type?.id)}
                                                disabled={submitting}
                                            />
                                        )}
                                    </Field>
                                    <Field<RevStateApproval> name="state">
                                        {({ input }) => (
                                            <FluentField label={intl.formatMessage(messages.state)} required>
                                                <Dropdown
                                                    disabled={submitting}
                                                    value={input.value ? RevStateApprovalCaption(input.value) : ''}
                                                    selectedOptions={input.value ? [input.value] : []}
                                                    onOptionSelect={(_event, data) =>
                                                        input.onChange(data.optionValue as RevStateApproval)
                                                    }
                                                >
                                                    {states.map((state) => (
                                                        <Option key={state} value={state}>
                                                            {RevStateApprovalCaption(state)}
                                                        </Option>
                                                    ))}
                                                </Dropdown>
                                            </FluentField>
                                        )}
                                    </Field>
                                    <Field<string> name="comment">
                                        {({ input }) => (
                                            <FluentField label={intl.formatMessage(messages.comment)}>
                                                <Textarea
                                                    disabled={submitting}
                                                    resize="vertical"
                                                    rows={4}
                                                    value={input.value ?? ''}
                                                    onChange={(_event, data) => input.onChange(data.value)}
                                                />
                                            </FluentField>
                                        )}
                                    </Field>
                                    <Field<number> name="assignedTo">
                                        {({ input, meta }) => (
                                            <AssignedUserField
                                                accessPointId={accessPointId}
                                                label={intl.formatMessage(messages.assignedUser)}
                                                value={input.value || undefined}
                                                onChange={input.onChange}
                                                disabled={submitting}
                                                error={meta.error}
                                                currentUserId={currentUserId}
                                                excludeUserIds={
                                                    values.state === RevStateApproval.TO_APPROVE
                                                        ? [currentUserId]
                                                        : undefined
                                                }
                                            />
                                        )}
                                    </Field>
                                </DialogContent>
                                <DialogActions>
                                    <Button type="submit" appearance="primary" disabled={submitting || !valid}>
                                        <FormattedMessage {...globalMessages.save} />
                                    </Button>
                                    <Button onClick={onClose} disabled={submitting}>
                                        <FormattedMessage {...globalMessages.cancel} />
                                    </Button>
                                </DialogActions>
                            </DialogBody>
                        </form>
                    )}
                </Form>
            </DialogSurface>
        </Dialog>
    );
};

export default RevStateChangeDialog;
