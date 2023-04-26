import React, { FC } from 'react';
import { Col, Row } from 'react-bootstrap';
import { Field, useForm, useField } from 'react-final-form';
import FormInput from '../../../../shared/form/FormInput';
import ReduxFormFieldErrorDecorator from '../../../../shared/form/ReduxFormFieldErrorDecorator';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import { ApItemUriRefVO } from 'api/ApItemUriRefVO';
import { CommonFieldProps } from './types';

export const FormUriRef:FC<CommonFieldProps<ApItemUriRefVO>> = ({
    name,
    label,
    disabled = false,
    disableRevision,
    onDelete = () => {console.warn("'onDelete' not defined")},
}) => {
    const form = useForm();
    const field = useField<RevisionItem<ApItemUriRefVO>>(`${name}`);
    const {item, updatedItem} = field.input.value;

    const descriptionField = useField(`${name}.updatedItem.description`);
    const valueField = useField(`${name}.updatedItem.value`)

    const isNew = updatedItem ? updatedItem.changeType === "NEW" || !updatedItem.changeType : false;
    const isDeleted = updatedItem?.changeType === "DELETED";

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
        if(!updatedItem){ throw Error("No updated item to revert."); }
        if(!item){ throw Error("No original item to revert to."); }

        const newUpdatedItem: ApItemUriRefVO = {...updatedItem, value: item?.value, description: item?.description, changeType: "ORIGINAL"};
        form.change(`${name}.updatedItem`, newUpdatedItem);
        handleValueUpdate(form);
    }

    const handleDelete = () => {
        if(disableRevision || isNew){onDelete()}
        else {
            form.change(`${name}.updatedItem`, {
                ...updatedItem,
                changeType: "DELETED",
                value: null,
                specId: undefined,
            })
        }
        handleValueUpdate(form);
    }

    return <Row>
        <Col xs={10}>
            <RevisionFieldExample
                label={label}
                prevValue={getValue(item)}
                value={getValue({
                    description: descriptionField.input.value,
                    value: valueField.input.value,
                })}
                disableRevision={disableRevision}
                onRevert={!isNew ? handleRevert : undefined}
                onDelete={isDeleted ? undefined : handleDelete}
                isDeleted={isDeleted}
            >
                <div style={{display: "flex"}}>
                    <Field
                        name={`${name}.updatedItem.description`}
                        label="Název"
                    >
                        {(props) => {
                            const handleBlur = (e: any) => {
                                props.input.onBlur(e)
                                handleValueUpdate(form, props);
                            }
                            const handleChange = (e: any) => {
                                if(updatedItem?.changeType === "ORIGINAL"){
                                    form.change(`${name}.updatedItem`, {...updatedItem, changeType: "UPDATED"})
                                }
                                props.input.onChange(e)
                            }


                            return <div style={{display: "flex", flexGrow: 1, flexDirection: "column"}}>
                            <ReduxFormFieldErrorDecorator
                                {...props as any}
                                input={{
                                    ...props.input,
                                    onChange: handleChange,
                                    onBlur: handleBlur // inject modified onChange handler
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
                            const handleBlur = (e: any) => {
                                props.input.onBlur(e)
                                handleValueUpdate(form, props);
                            }
                            const handleChange = (e: any) => {
                                if(updatedItem?.changeType === "ORIGINAL"){
                                    form.change(`${name}.updatedItem`, {...updatedItem, changeType: "UPDATED"})
                                }
                                props.input.onChange(e)
                            }

                            return <div style={{display: "flex", flexGrow: 2, flexDirection: "column"}}>
                                <ReduxFormFieldErrorDecorator
                                {...props as any}
                                input={{
                                    ...props.input,
                                    onChange: handleChange,
                                    onBlur: handleBlur // inject modified onChange handler
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
