import React, { memo, useCallback, useState } from 'react';
import { Form } from 'react-bootstrap';
import { FieldArrayFieldsProps } from 'redux-form';

type Record = {id: string, checked: boolean};
type PolicyTypeItem = {code: string, name: string, ruleSetId: number};

interface PolicyTypeItems {
    [x: number]: PolicyTypeItem
}

interface ICheckboxArrayFieldProps {
    name: string
    fields: FieldArrayFieldsProps<Record>
    items: PolicyTypeItems
    disabled: boolean
}

export const CheckboxArrayField: React.FC<ICheckboxArrayFieldProps> = memo(({fields, ...props}) => (
    <>
        {fields.map((field, index) => {
            return (
                <RecordField
                    key={index}
                    index={index}
                    fields={fields}
                    {...props}
                />
            );
        })}
    </>
));

interface IRecordFieldProps extends ICheckboxArrayFieldProps {
    index: number
}

const RecordField: React.FC<IRecordFieldProps> = memo(({index, fields, items, ...props}) => {

    const [id] = useState(fields.get(index).id);

    const onChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
        fields.remove(index);
        fields.insert(index, {id, checked: event.currentTarget.checked});
    }, [fields, index, id]);

    return (
        <Form.Check
            checked={fields.get(index).checked}
            onChange={onChange}
            label={items[id]?.name}
            {...props}
        />
    );
});
