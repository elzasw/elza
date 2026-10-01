import { Dialog, DialogSurface, DialogTitle } from '@fluentui/react-components';
import { ComponentType } from 'react';
import { defineMessages, useIntl } from 'react-intl';
import { userPasswordChange } from 'actions/admin/user.jsx';
import PasswordFormUntyped from 'components/admin/PasswordForm';
import { useAppSelector, useAppThunkDispatch } from 'utils/hooks';

const messages = defineMessages({
    // same text as the admin password change dialog
    title: { id: 'admin.user.passwordChange.title', defaultMessage: 'Změna hesla' },
});

interface PasswordValues {
    oldPassword: string;
    password: string;
}

// redux-form component written in JS; its inferred props do not include its own props
const PasswordForm = PasswordFormUntyped as unknown as ComponentType<{
    forced?: boolean;
    onSubmitForm: (data: PasswordValues) => Promise<unknown>;
}>;

/**
 * Non-dismissible password change after a login with an expired password,
 * a password change required by the administrator, or a recovery login.
 * Closes itself once the reloaded user detail no longer requires the change.
 */
export function ForcedPasswordChange() {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const logged = useAppSelector(state => state.login.logged);
    const needChangePassword = useAppSelector(state => state.userDetail.needChangePassword);

    const handleSubmit = (data: PasswordValues) => dispatch(userPasswordChange(data.oldPassword, data.password));

    return (
        <Dialog open={logged && !!needChangePassword} modalType="alert">
            <DialogSurface>
                <DialogTitle>{intl.formatMessage(messages.title)}</DialogTitle>
                <PasswordForm forced onSubmitForm={handleSubmit} />
            </DialogSurface>
        </Dialog>
    );
}
