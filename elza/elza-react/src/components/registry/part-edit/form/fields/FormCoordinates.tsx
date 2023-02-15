import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import { WebApi } from 'actions/WebApi';
import classNames from 'classnames';
import { Icon } from 'components';
import i18n from 'components/i18n';
import FormInput from 'components/shared/form/FormInput';
import ReduxFormFieldErrorDecorator from 'components/shared/form/ReduxFormFieldErrorDecorator';
import React, { FC } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { Field, useForm, useField } from 'react-final-form';
import { useDispatch } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { AppState } from 'typings/store';
import ImportCoordinateModal from '../../../Detail/coordinate/ImportCoordinateModal';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import { ApItemCoordinatesVO } from 'api/ApItemCoordinatesVO';
import { CommonFieldProps } from './types';
import { wktFromTypeAndData } from 'components/Utils';

type ThunkAction<R> = (dispatch: ThunkDispatch<AppState, void, AnyAction>, getState: () => AppState) => Promise<R>;
const useThunkDispatch = <State,>():ThunkDispatch<State, void, AnyAction> => useDispatch()

export const FormCoordinates:FC<CommonFieldProps<ApItemCoordinatesVO>> = ({
    name,
    label,
    disabled = false,
    disableRevision,
    onDelete = () => {console.warn("'onDelete' not defined")},
}) => {
    const fieldName = `${name}.updatedItem.value`;
    const dispatch = useThunkDispatch<AppState>()
    const form = useForm();
    const field = useField<RevisionItem>(`${name}`);
    const {updatedItem, item} = field.input.value;
    const prevValue = (item as ApItemCoordinatesVO | undefined)?.value;

    const handleImport = async () => {
        const fieldValue = await dispatch(importCoordinateFile());
        form.change(fieldName, fieldValue)
    }

    return <Row>
        <Col>
            <Field
                name={fieldName}
            >
                {(props) => {
                    const isNew = updatedItem ? updatedItem.changeType === "NEW" || (!item && !!updatedItem) : false;
                    const isDeleted = updatedItem?.changeType === "DELETED";

                    const handleBlur = (e: any) => {
                        props.input.onBlur(e)

                        // convert value with point coordinates to wkt when possible
                        // if not, keeps current value
                        const value = wktFromTypeAndData("POINT", e.target.value);
                        form.change(`${name}.updatedItem`, {...updatedItem, value})

                        handleValueUpdate(form, props);
                    }

                    const handleRevert = () => {
                        form.change(`${name}.updatedItem`, item)
                        handleValueUpdate(form, props);
                    }

                    const handleDelete = () => {
                        if(disableRevision || isNew){onDelete()}
                        else {
                            form.change(`${name}.updatedItem`, {
                                ...updatedItem,
                                changeType: "DELETED",
                                value: null,
                            })
                        }
                        handleValueUpdate(form);
                    }

                    return <RevisionFieldExample
                        label={label}
                        prevValue={prevValue}
                        disableRevision={disableRevision}
                        value={props.input.value}
                        onRevert={!isNew ? handleRevert : undefined}
                        onDelete={isDeleted ? undefined : handleDelete}
                        isDeleted={isDeleted}
                        equalSplit={true}
                        alignTop={true}
                    >
                        <div style={{display: "flex"}}>
                            <div style={{flexGrow: 1}}>
                            <ReduxFormFieldErrorDecorator
                                {...props as any}

                                input={{
                                    ...props.input,
                                    onBlur: handleBlur // inject modified onBlur handler
                                }}
                                disabled={disabled}
                                renderComponent={FormInput}
                                as="textarea"
                                />
                            </div>
                            <Button
                                variant={'action' as any}
                                className={classNames('side-container-button', 'm-1')}
                                title={'Importovat'}
                                onClick={handleImport}
                            >
                                <Icon glyph={'fa-file'} />
                            </Button>
                        </div>
                    </RevisionFieldExample>

                }}
            </Field>
        </Col>
    </Row>
}

const importCoordinateFile = ():ThunkAction<any> =>
(dispatch) => new Promise((resolve) =>
    dispatch(
        modalDialogShow(
            this,
            i18n('ap.coordinate.import.title'),
            <ImportCoordinateModal
                onSubmit={async (formData) => {
                    const data = await readFileAsBinaryString(formData.file)
                    try {
                        const fieldValue = await WebApi.importApCoordinates(data!, formData.format);
                        console.log(fieldValue);
                        resolve(fieldValue);
                    } catch (error) {
                        console.error(error)
                    }
                }}
                onSubmitSuccess={(result, dispatch) => {
                    console.log(result);
                    dispatch(modalDialogHide())
                }}
                />
        ))
)

const readFileAsBinaryString = (file: File) => {
    return new Promise<string | ArrayBuffer | null>((resolve)=>{
        const reader = new FileReader();
        reader.onload = async () => {
            resolve(reader.result);
        };
        reader.readAsBinaryString(file);
    })
}
