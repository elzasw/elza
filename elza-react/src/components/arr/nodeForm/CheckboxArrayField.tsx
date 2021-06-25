import React, { PropsWithChildren, useCallback, useState } from 'react';
import { Form } from 'react-bootstrap';
import { Field, FieldArrayFieldsProps, WrappedFieldProps } from 'redux-form';
import { VisiblePolicyRefItem } from "../../../typings/store";

type RecordType = {id: string, checked: boolean};

interface ItemWithId {
    id: number | string;
    name: string;
}

interface ICheckboxArrayFieldProps {
    name: string;
    fields: FieldArrayFieldsProps<RecordType>;
    items: VisiblePolicyRefItem[];
    item: VisiblePolicyRefItem;
    disabled: boolean;
}

interface CheckboxGroupProps<T extends ItemWithId> {
    name: string;
    items: T[];
    disabled?: boolean;
}

export const CheckboxGroup = <T extends ItemWithId>({
    disabled = false,
    items,
    name,
}: PropsWithChildren<CheckboxGroupProps<T>>) => {
    const renderField = (props:WrappedFieldProps)=>{
        const {input: { value, onChange }} = props;

        const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
            const newVal = {
                ...value,
                [e.target.id]: !value[e.target.id] || false,
            };
            onChange(newVal);
        }

        return <div>
            {items.map(({id, name})=>{
                return <Form.Check
                    id={id.toString()}
                    key={id}
                    checked={value[id] || false}
                    onChange={handleChange}
                    label={name}
                    disabled={disabled}
                />
            })}
        </div>
    }

    return <Field 
        name={name} 
        component={renderField}
    />
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
