import React, { PropsWithChildren } from "react";
import { Field, WrappedFieldProps } from "redux-form";
import { CheckboxGroupItem } from "./CheckboxGroupItem";

interface ItemWithId {
    id: number | string;
    name: string;
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
                return <CheckboxGroupItem 
                    label={name}
                    id={id.toString()}
                    key={id}
                    checked={value[id] || false}
                    onChange={handleChange}
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
