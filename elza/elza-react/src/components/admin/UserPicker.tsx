import { Combobox, Field, Option } from '@fluentui/react-components';
import { WebApi } from 'actions';
import { UsrUserVO } from 'api/UsrUserVO';
import { useEffect, useRef, useState } from 'react';
import { defineMessages, useIntl } from 'react-intl';

const messages = defineMessages({
    noMatch: {
        id: 'admin.userPicker.noMatch',
        defaultMessage: 'Žádný uživatel neodpovídá',
    },
});

const MAX_USERS = 200;

export interface UserPickerProps {
    /** Id of the selected user. */
    value?: number;
    onChange: (userId: number | undefined) => void;
    label: string;
    disabled?: boolean;
    /** Validation message shown under the field. */
    error?: string;
    /** These users are not offered. */
    excludeUserIds?: number[];
}

const userName = (user: UsrUserVO) => `${user.accessPoint?.name} (${user.username})`;

/**
 * A user searched by name: typing asks the server, the selected user can be cleared. The value is
 * the user id; a user not among the offered ones is loaded to show the name.
 */
export const UserPicker = ({ value, onChange, label, disabled, error, excludeUserIds = [] }: UserPickerProps) => {
    const intl = useIntl();
    const [users, setUsers] = useState<UsrUserVO[]>([]);
    const [selected, setSelected] = useState<UsrUserVO | undefined>();
    const [query, setQuery] = useState('');
    const searchId = useRef(0);

    useEffect(() => {
        if (value == null) {
            setSelected(undefined);
            setQuery('');
            return;
        }
        if (selected?.id === value) {
            return;
        }
        const known = users.find(({ id }) => id === value);
        if (known) {
            setSelected(known);
            setQuery(userName(known));
            return;
        }
        let cancelled = false;
        WebApi.getUser(value).then((user: UsrUserVO) => {
            if (!cancelled) {
                setSelected(user);
                setQuery(userName(user));
            }
        });
        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value]);

    const search = (text: string) => {
        const id = ++searchId.current;
        WebApi.findUser(text.trim() || null, true, false, MAX_USERS, null, undefined, undefined, true).then(
            ({ data }) => {
                // an older answer must not overwrite a newer one
                if (id === searchId.current) {
                    setUsers(data ?? []);
                }
            }
        );
    };

    const offered = users.filter(({ id }) => !excludeUserIds.includes(id));

    return (
        <Field label={label} validationMessage={error} validationState={error ? 'error' : 'none'}>
            <Combobox
                value={query}
                selectedOptions={value != null ? [String(value)] : []}
                disabled={disabled}
                clearable
                freeform
                positioning={{ autoSize: 'height' }}
                onOpenChange={(_event, data) => {
                    if (data.open) {
                        search(query === (selected ? userName(selected) : '') ? '' : query);
                    } else if (selected) {
                        // leaving the field with an unfinished text keeps the selected user
                        setQuery(userName(selected));
                    }
                }}
                onChange={(event) => {
                    setQuery(event.target.value);
                    search(event.target.value);
                }}
                onOptionSelect={(_event, data) => {
                    if (!data.optionValue) {
                        // the clear button
                        setSelected(undefined);
                        setQuery('');
                        onChange(undefined);
                        return;
                    }
                    const user = users.find(({ id }) => String(id) === data.optionValue);
                    if (user) {
                        setSelected(user);
                        setQuery(userName(user));
                        onChange(user.id);
                    }
                }}
            >
                {offered.map((user) => (
                    <Option key={user.id} value={String(user.id)} text={userName(user)}>
                        {userName(user)}
                    </Option>
                ))}
                {offered.length === 0 && query.trim() !== '' && (
                    <Option disabled value="" text="">
                        {intl.formatMessage(messages.noMatch)}
                    </Option>
                )}
            </Combobox>
        </Field>
    );
};

export default UserPicker;
