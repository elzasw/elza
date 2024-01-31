import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import { WebApi } from 'actions/WebApi';
import classNames from 'classnames';
import { Icon } from 'components';
import i18n from 'components/i18n';
import FormInput from 'components/shared/form/FormInput';
import ReduxFormFieldErrorDecorator from 'components/shared/form/ReduxFormFieldErrorDecorator';
import React, { FC, useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { Field, useForm, useField } from 'react-final-form';
import { useDispatch, useSelector } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { AppState, ExternalSystem } from 'typings/store';
import ImportCoordinateModal from '../../../Detail/coordinate/ImportCoordinateModal';
import { handleValueUpdate } from '../valueChangeMutators';
import { RevisionFieldExample, RevisionItem } from '../../../revision';
import { ApItemCoordinatesVO } from 'api/ApItemCoordinatesVO';
import { CommonFieldProps } from './types';
import { wktFromTypeAndData } from 'components/Utils';
import { MapEditor } from './coordinates/MapEditor';
import { GisSystemType } from '../../../../../constants';
import { kmlExtSystemListFetchIfNeeded } from 'actions/admin/kmlExtSystemList';

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
    const field = useField<RevisionItem<ApItemCoordinatesVO>>(`${name}`);
    const {updatedItem, item} = field.input.value;
    const prevValue = item?.value;

    const refDescItemTypesMap = useSelector((state: AppState) => state.refTables.descItemTypes.itemsMap)

    const geoEditExternalSystems = useSelector((state: AppState) => {
        return state.app.kmlExtSystemList.rows.filter((extSystem) => {
            if (extSystem.type === GisSystemType.FrameApiEdit) {
                return true;
            }
        })
    });

    useEffect(() => {
        dispatch(kmlExtSystemListFetchIfNeeded());
    }, [])

    const getAllowedGeometryTypes = () => {
        const _item = updatedItem || item;
        if(!_item){return undefined;}

        const allowedGeometryTypes:string[] = [];
        const refDescItemType = refDescItemTypesMap[_item.typeId];
        if(refDescItemType.code.toLowerCase().indexOf("point") >= 0){
            allowedGeometryTypes.push("POINT");
        }
        return allowedGeometryTypes;
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

                    const handleChange = (e: any) => {
                        if(updatedItem?.changeType === "ORIGINAL"){
                            form.change(`${name}.updatedItem`, {...updatedItem, changeType: "UPDATED"})
                        }
                        props.input.onChange(e)
                    }

                    const handleRevert = () => {
                        if(!updatedItem){ throw Error("No updated item to revert."); }
                        if(!item){ throw Error("No original item to revert to."); }

                        const newUpdatedItem: ApItemCoordinatesVO = {...updatedItem, value: item?.value, changeType: "ORIGINAL"};
                        form.change(`${name}.updatedItem`, newUpdatedItem);
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

                    const handleImport = async () => {
                        const value = await dispatch(importCoordinateFile());
                        form.change(`${name}.updatedItem`, {
                            ...updatedItem,
                            changeType: "UPDATED",
                            value
                        })
                    }

                    const handleEditInMap = async () => {
                        if (geoEditExternalSystems.length === 1) {
                            const value = await dispatch(editInMapEditor(props.input.value, geoEditExternalSystems[0], getAllowedGeometryTypes()))
                            form.change(`${name}.updatedItem`, {
                                ...updatedItem,
                                changeType: "UPDATED",
                                value
                            })
                        } else {
                            throw Error("Missing or multiple GIS editing external systems. Has to be exactly one.")
                        }

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
                                    onChange: handleChange,
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
                            {geoEditExternalSystems.length === 1 && <Button
                                variant={'action' as any}
                                className={classNames('side-container-button', 'm-1')}
                                title={i18n('ap.coordinate.edit-in-map')}
                                onClick={handleEditInMap}
                            >
                                <Icon glyph={'fa-map'} />
                            </Button>
                            }
                        </div>
                    </RevisionFieldExample>

                }}
            </Field>
        </Col>
    </Row>
}

export const editInMapEditor = (geometry: string, extSystem: ExternalSystem, allowedGeometryTypes?: string[]): ThunkAction<any> =>
    (dispatch) => new Promise((resolve) => {
        const handleEditorChange = (value: string) => {
            resolve(value);
            dispatch(modalDialogHide());
        }

        dispatch(modalDialogShow(
            this,
            i18n('ap.coordinate.map-editor.title'),
            ({key, onClose, visible}) => {
                return <MapEditor
                    key={key}
                    onClose={onClose}
                    geometry={geometry}
                    extSystem={extSystem}
                    onChange={handleEditorChange}
                    allowedGeometryTypes={allowedGeometryTypes}
                />
            }
        ))
    })

const importCoordinateFile = (): ThunkAction<any> =>
    (dispatch) => new Promise((resolve) =>
        dispatch(
            modalDialogShow(
                this,
                i18n('ap.coordinate.import.title'),
                <ImportCoordinateModal
                    onSubmit={async (formData) => {
                        try {
                            const fieldValue = await WebApi.importApCoordinates(formData.file, formData.format);
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
