import React, { FC } from 'react';
import { Col, Row } from 'react-bootstrap';
import { Field, useForm, useField } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import { ApItemUriRefVO } from 'api/ApItemUriRefVO';

export const FormUriRef:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    prevItem?: ApItemUriRefVO;
    disableRevision?: boolean;
}> = ({
    name,
    label,
    disabled = false,
    disableRevision,
}) => {
    const form = useForm();
    const field = useField<RevisionItem>(`${name}`);
    const {item, updatedItem} = field.input.value;
    const descriptionField = useField(`${name}.updatedItem.description`);
    const valueField = useField(`${name}.updatedItem.value`)
    const validate = (value:string) => {
        if(!value?.match(/^.+:.+$/g)){
            return "Nesprávný formát odkazu";
        }
        return undefined;
    }

    const getValue = (item?: {description: string, value: string}) => {
        if(!item){return undefined}
        return `${item.description}: ${item.value}`
    }

    const handleRevert = () => {
        form.change(`${name}.updatedItem`, item)
        handleValueUpdate(form);
    }

    const handleDelete = () => {
        form.change(`${name}.updatedItem`, {
            ...updatedItem,
            changeType: "DELETED",
            value: null,
        })
        handleValueUpdate(form);
    }

    const isNew = updatedItem ? updatedItem.changeType === "NEW" || !updatedItem.changeType : false;
    const isDeleted = updatedItem?.changeType === "DELETED";

    return <Row>
        <Col xs={10}>
            <RevisionFieldExample
                label={label}
                prevValue={getValue(item as ApItemUriRefVO)}
                value={getValue({
                    description: descriptionField.input.value,
                    value: valueField.input.value,
                })}
                disableRevision={disableRevision}
                onRevert={!isNew ? handleRevert : undefined}
                onDelete={disableRevision || isNew || isDeleted ? undefined : handleDelete}
                isDeleted={isDeleted}
            >
                <div style={{display: "flex"}}>
                    <Field
                        name={`${name}.updatedItem.description`}
                        label="Název"
                    >
                        {(props) => {
                            const handleChange = (e: any) => {
                                props.input.onBlur(e)
                                handleValueUpdate(form, props);
                            }


                            return <div style={{display: "flex", flexGrow: 1, flexDirection: "column"}}>
                            <ReduxFormFieldErrorDecorator
                                {...props as any}
                                input={{
                                    ...props.input,
                                    onBlur: handleChange // inject modified onChange handler
                                }}
                                disabled={disabled}
                                maxLength={250}
                                renderComponent={FormInput}
                                />
                                </div>

                        }}
                    </Field>
                    <Field
                        name={`${name}.updatedItem.value`}
                        label={"Odkaz"}
                        validate={validate}
                    >
                        {(props) => {
                            const handleChange = (e: any) => {
                                props.input.onBlur(e)
                                handleValueUpdate(form, props);
                            }

                            return <div style={{display: "flex", flexGrow: 2, flexDirection: "column"}}>
                                <ReduxFormFieldErrorDecorator
                                {...props as any}
                                input={{
                                    ...props.input,
                                    onBlur: handleChange // inject modified onChange handler
                                }}
                                disabled={disabled}
                                maxLength={1000}
                                renderComponent={FormInput}
                                />
                            </div>

                        }}
                    </Field>
                </div>
            </RevisionFieldExample>
        </Col>
    </Row>
}
