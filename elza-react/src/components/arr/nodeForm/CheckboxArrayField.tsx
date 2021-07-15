import React, { useCallback, useState } from 'react';
import { Form } from 'react-bootstrap';
import { FieldArrayFieldsProps } from 'redux-form';
import { VisiblePolicyRefItem } from "../../../typings/store";

type RecordType = {id: string, checked: boolean};

interface ICheckboxArrayFieldProps {
    name: string;
    fields: FieldArrayFieldsProps<RecordType>;
    items: VisiblePolicyRefItem[];
    item: VisiblePolicyRefItem;
    disabled: boolean;
}

export const CheckboxArrayField: React.FC<ICheckboxArrayFieldProps> = ({fields, ...props}) => (
    <>
        {props.items?.map((item, index) => {
            return (
                <RecordField
                    {...props}
                    key={index}
                    index={index}
                    fields={fields}
                    item={item}
                />
            );
        })}
    </>
);

interface IRecordFieldProps extends ICheckboxArrayFieldProps {
    index: number
}

const RecordField: React.FC<IRecordFieldProps> = ({
    index, 
    fields, 
    items, 
    item,
    ...props
}) => {

    const [id] = useState(fields.get(index).id);

    const onChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
        fields.remove(index);
        fields.insert(index, {id, checked: event.currentTarget.checked});
    }, [fields, index, id]);

    return (
        <Form.Check
            {...props}
            checked={fields.get(index).checked}
            onChange={onChange}
            label={item.name}
        />
    );
};
