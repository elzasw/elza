import React, {FC} from 'react';
import classNames from 'classnames';
import { Field, useForm } from 'react-final-form';
import {Button, Col, Row} from 'react-bootstrap';
import {Icon} from 'components';
import ReduxFormFieldErrorDecorator from 'components/shared/form/ReduxFormFieldErrorDecorator';
import FormInput from 'components/shared/form/FormInput';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog';
import {WebApi} from 'actions/WebApi';
import ImportCoordinateModal from '../../../Detail/coordinate/ImportCoordinateModal';
import i18n from 'components/i18n';
import { ThunkDispatch } from 'redux-thunk';
import { AnyAction } from 'redux';
import { useDispatch } from 'react-redux';
import {AppState} from 'typings/store';

type ThunkAction<R> = (dispatch: ThunkDispatch<AppState, void, AnyAction>, getState: () => AppState) => Promise<R>;
const useThunkDispatch = <State,>():ThunkDispatch<State, void, AnyAction> => useDispatch()

export const FormCoordinates:FC<{
    name: string;
    label: string;
    disabled?: boolean;
    // onImport?: (name: string) => void;
}> = ({
    name,
    label,
    disabled = false,
    // onImport = () => console.warn("'onImport' undefined.")
}) => {
    const fieldName = `${name}.value`;
    const form = useForm();
    const dispatch = useThunkDispatch<AppState>()
    const handleImport = async () => {
        const fieldValue = await dispatch(importCoordinateFile());
        form.change(fieldName, fieldValue)
    }
    return <Row>
        <Col>
            <Field
                name={fieldName}
                label={label}
            >
                {(props) => {
                    const handleChange = (e: any) => { 
                        props.input.onBlur(e)
                        form.mutators.attributes?.(name);
                    }

                    return <ReduxFormFieldErrorDecorator
                        {...props as any}
                        input={{
                            ...props.input,
                            onBlur: handleChange // inject modified onChange handler
                        }}
                        disabled={disabled}
                        renderComponent={FormInput}
                        as="textarea"
                        />

                }}
            </Field>
        </Col>
        <Col xs="auto" className="action-buttons">
            {/*TODO: az bude na serveru */}
            <Button
                variant={'action' as any}
                className={classNames('side-container-button', 'm-1')}
                title={'Importovat'}
                onClick={handleImport}
            >
                <Icon glyph={'fa-download'} />
            </Button>
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
                onSubmit={async formData => {
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
