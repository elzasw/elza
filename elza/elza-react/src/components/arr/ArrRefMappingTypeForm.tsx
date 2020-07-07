import * as React from 'react';
import {
    formValueSelector,
    InjectedFormProps,
    reduxForm,
    FormErrors,
    Field,
    FieldArray,
    WrappedFieldArrayProps,
} from 'redux-form';
import {ArrRefTemplateMapSpecVO, ArrRefTemplateMapTypeVO} from '../../types';
import {Button, Form, Modal, Row, Col} from 'react-bootstrap';
import FormInputField from '../shared/form/FormInputField';
import i18n from '../i18n';
import Icon from '../shared/icon/Icon';
import FF from '../shared/form/FF';
import DescItemTypeField from './DescItemTypeField';
import ItemSpecField from './ItemSpecField';
import {connect} from 'react-redux';

type OwnProps = {create?: boolean};
type ConnectedProps = ReturnType<typeof mapStateToProps>;
type Props = ConnectedProps &
    OwnProps &
    InjectedFormProps<ArrRefTemplateMapTypeVO, OwnProps, FormErrors<ArrRefTemplateMapTypeVO>>;

class ArrRefMappingTypeForm extends React.Component<Props> {
    render() {
        const {handleSubmit, pristine, submitting, create, fromItemTypeId, toItemTypeId} = this.props;
        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <h3>{i18n('arr.refTemplates.mapping.apMapping')}</h3>
                    <Row>
                        <Col>
                            <FF
                                name="fromItemTypeId"
                                field={DescItemTypeField}
                                label={i18n('arr.refTemplates.mapping.fromItemTypeId')}
                                useIdAsValue
                            />

                            <Field
                                name="fromParentLevel"
                                type="checkbox"
                                component={FormInputField}
                                label={i18n('arr.refTemplates.mapping.fromParentLevel')}
                                disabled={submitting}
                            />
                        </Col>
                        <Col>
                            <FF
                                name="toItemTypeId"
                                field={DescItemTypeField}
                                label={i18n('arr.refTemplates.mapping.toItemTypeId')}
                                useIdAsValue
                            />
                            <Field
                                name="mapAllSpec"
                                type="checkbox"
                                component={FormInputField}
                                label={i18n('arr.refTemplates.mapping.mapAllSpec')}
                                disabled={submitting}
                            />
                        </Col>
                    </Row>
                    <FieldArray
                        name={'refTemplateMapSpecVOList'}
                        component={({fields, meta}: WrappedFieldArrayProps<ArrRefTemplateMapSpecVO>) => (
                            <>
                                <h3>
                                    {i18n('arr.refTemplates.mapping.specMapping')}{' '}
                                    <Button variant={'action' as any} onClick={() => fields.push({} as any)}>
                                        <Icon glyph={'fa-plus'} />
                                    </Button>
                                </h3>
                                {fields.map((name, index, fields) => {
                                    return (
                                        <>
                                            {index > 0 && <hr />}
                                            <Row>
                                                <Col>
                                                    <FF
                                                        name={`${name}.fromItemSpecId`}
                                                        field={ItemSpecField}
                                                        label={i18n('arr.refTemplates.mapping.fromItemSpecId')}
                                                        itemTypeId={fromItemTypeId}
                                                        useIdAsValue
                                                    />
                                                </Col>
                                                <Col>
                                                    <FF
                                                        name={`${name}.toItemSpecId`}
                                                        field={ItemSpecField}
                                                        label={i18n('arr.refTemplates.mapping.toItemSpecId')}
                                                        itemTypeId={toItemTypeId}
                                                        useIdAsValue
                                                    />
                                                </Col>
                                                <Col xs={'auto'} className={'align-items-end d-flex'}>
                                                    <Button
                                                        variant={'action' as any}
                                                        onClick={() => fields.remove(index)}
                                                    >
                                                        <Icon glyph={'fa-trash'} />
                                                    </Button>
                                                </Col>
                                            </Row>
                                        </>
                                    );
                                })}
                            </>
                        )}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {create ? i18n('global.action.create') : i18n('global.action.update')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

function mapStateToProps(state, props) {
    const formSelector = formValueSelector(props.form || 'ArrRefMappingTypeForm');
    return {
        fromItemTypeId: formSelector(state, 'fromItemTypeId'),
        toItemTypeId: formSelector(state, 'toItemTypeId'),
    };
}

export default connect(mapStateToProps)(
    reduxForm<ArrRefTemplateMapTypeVO, OwnProps, FormErrors<ArrRefTemplateMapTypeVO>>({
        form: 'ArrRefMappingTypeForm',
        initialValues: {
            fromItemTypeId: undefined,
            fromParentLevel: undefined,
            toItemTypeId: undefined,
            mapAllSpec: undefined,
            refTemplateMapSpecVOList: [],
        },
    })(ArrRefMappingTypeForm),
);
