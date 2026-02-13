import { forwardRef, useEffect, useState } from 'react';
import {WebApi} from 'actions/index.jsx';
import {Autocomplete} from 'components/shared';
import {renderUserItem} from './adminRenderUtils.jsx';
import { UsrUserVO } from 'api/UsrUserVO.js';

interface MinimalUser {
    accessPoint: { name: string };
    username: string;
    id: number
}

export interface Props {
    value?: MinimalUser | UsrUserVO | number;
    onChange?: (user: UsrUserVO) => void;
    inline?: boolean;
    touched?: boolean;
    error?: boolean;
    tags?: boolean;
    excludedGroupId?: number;
    all?: boolean;
    disabled?: boolean;
    excludeUserIds?: number[];
}

function isMinimalUser(value?: UsrUserVO | number | MinimalUser): value is MinimalUser{
    return typeof value === 'object' && value?.id != undefined && value?.accessPoint != undefined
}

export const UserField = forwardRef<Autocomplete, Props>(({
    value,
    onChange,
    inline,
    touched,
    error,
    tags,
    excludedGroupId,
    all,
    disabled,
    excludeUserIds = [],
    ...otherProps
}:Props, ref) => {
    const [items, setItems] = useState<UsrUserVO[]>([]);
    const [_value, setValue] = useState<MinimalUser | undefined>(isMinimalUser(value) ? value : undefined);
    const [loadingUserId, setLoadingUserId] = useState<number>();

    useEffect(() => {
        // clear value when undefined
        if (value === undefined) {
            setValue(undefined);
        }
        // use the external value when it is of type UsrUserVO
        else if(isMinimalUser(value)) {
            setValue(value);
        }
        // load the UsrUserVO value when the external value is id
        else if(typeof value === 'number' && _value?.id !== value && loadingUserId == undefined) {
            (async () => {
                setLoadingUserId(value);
                const user = await WebApi.getUser(value);
                setValue(user);
                setLoadingUserId(undefined);
            })()
        }
    }, [value, _value, loadingUserId])

    function handleSearchChange(query: string){
        query = query === '' ? null : query;

        WebApi.findUser(query, true, false, 200, excludedGroupId, undefined, undefined, all).then(json => {
            const _items = json.data?.filter(({id}) => excludeUserIds.indexOf(id) < 0)
            setItems(_items);
        });
    }

    function handleChange(value: UsrUserVO) {
        onChange(value);
    }

    return (
        <Autocomplete
            {...otherProps}
            disabled={disabled}
            tags={tags}
            ref={ref}
            className="form-group"
            customFilter
            value={_value}
            items={items}
            onSearchChange={handleSearchChange}
            error={error}
            touched={touched}
            inline={inline}
            onChange={handleChange}
            renderItem={renderUserItem}
            getItemName={(user: UsrUserVO) => {
                if(!user?.id){
                    return "";
                }
                return `${user.accessPoint?.name} (${user.username})`;
            }}
        />
    );
})

export default UserField;
