import * as React from 'react';
import {
    formValueSelector,
    InjectedFormProps,
    reduxForm,
    FormErrors,
    Field,
    FieldArray,
    WrappedFieldArrayProps,
    DecoratedFormProps,
} from 'redux-form';
import {ArrRefTemplateMapSpecVO, ArrRefTemplateMapTypeVO} from '../../types';
import {Button, Form, Modal, Row, Col} from 'react-bootstrap';
import FormInputField from '../shared/form/FormInputField';
import { FormattedMessage, injectIntl, WrappedComponentProps } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { templateMessages } from './templateMessages';
import Icon from '../shared/icon/Icon';
import FF from '../shared/form/FF';
import DescItemTypeField from './DescItemTypeField';
import ItemSpecField from './ItemSpecField';
import {connect} from 'react-redux';
import requireFields from '../../shared/utils/requireFields';

type OwnProps = {create?: boolean};
type ConnectedProps = ReturnType<typeof mapStateToProps>;
type Props = ConnectedProps &
    OwnProps &
    InjectedFormProps<ArrRefTemplateMapTypeVO, OwnProps, FormErrors<ArrRefTemplateMapTypeVO>>;

const MapTypesField = ({
    fields,
    meta,
    fromItemTypeId,
    toItemTypeId,
}: WrappedFieldArrayProps<ArrRefTemplateMapSpecVO> & {fromItemTypeId: number; toItemTypeId: number}) => (
    <>
        <h3>
            {<FormattedMessage {...templateMessages.refTemplatesMappingSpecMapping} />}{' '}
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
                                label={<FormattedMessage {...templateMessages.refTemplatesMappingFromItemSpecId} />}
                                itemTypeId={fromItemTypeId}
                                useIdAsValue
                            />
                        </Col>
                        <Col>
                            <FF
                                name={`${name}.toItemSpecId`}
                                field={ItemSpecField}
                                label={<FormattedMessage {...templateMessages.refTemplatesMappingToItemSpecId} />}
                                itemTypeId={toItemTypeId}
                                useIdAsValue
                            />
                        </Col>
                        <Col xs={'auto'} className={'d-flex'}>
                            <Button variant={'action' as any} onClick={() => fields.remove(index)} className={'mt-4'}>
                                <Icon glyph={'fa-trash'} />
                            </Button>
                        </Col>
                    </Row>
                </>
            );
        })}
    </>
);

class ArrRefMappingTypeForm extends React.Component<Props & WrappedComponentProps> {
    render() {
        const {handleSubmit, pristine, submitting, create, fromItemTypeId, toItemTypeId, valid} = this.props;
        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <Row>
                        <Col>
                            <FF
                                name="fromItemTypeId"
                                field={DescItemTypeField}
                                label={<FormattedMessage {...templateMessages.refTemplatesMappingFromItemTypeId} />}
                                useIdAsValue
                            />

                            <Field
                                name="fromParentLevel"
                                type="checkbox"
                                component={FormInputField}
                                label={<FormattedMessage {...templateMessages.refTemplatesMappingFromParentLevel} />}
                                disabled={submitting}
                            />
                        </Col>
                        <Col>
                            <FF
                                name="toItemTypeId"
                                field={DescItemTypeField}
                                label={<FormattedMessage {...templateMessages.refTemplatesMappingToItemTypeId} />}
                                useIdAsValue
                            />
                            <Field
                                name="mapAllSpec"
                                type="checkbox"
                                component={FormInputField}
                                label={<FormattedMessage {...templateMessages.refTemplatesMappingMapAllSpec} />}
                                disabled={submitting}
                            />
                        </Col>
                    </Row>
                    <FieldArray
                        name={'refTemplateMapSpecVOList'}
                        component={MapTypesField}
                        fromItemTypeId={fromItemTypeId}
                        toItemTypeId={toItemTypeId}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting || !valid}>
                        {create ? this.props.intl.formatMessage(globalMessages.create) : this.props.intl.formatMessage(globalMessages.save)}
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

const subLevelValidation = requireFields('fromItemSpecId', 'toItemSpecId');
const topValidation = requireFields('fromItemTypeId', 'toItemTypeId');

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
        validate(
            values: ArrRefTemplateMapTypeVO,
            props: DecoratedFormProps<ArrRefTemplateMapTypeVO, OwnProps, FormErrors<ArrRefTemplateMapTypeVO>>,
        ): FormErrors<ArrRefTemplateMapTypeVO, FormErrors<ArrRefTemplateMapTypeVO>> {
            const errors: FormErrors<ArrRefTemplateMapTypeVO, FormErrors<ArrRefTemplateMapTypeVO>> = topValidation(
                values,
            );

            if (values.refTemplateMapSpecVOList && values.refTemplateMapSpecVOList.length > 0) {
                errors.refTemplateMapSpecVOList = values.refTemplateMapSpecVOList.map(subLevelValidation) as any;
            }

            console.log('ddd', errors);
            return errors;
        },
    })(injectIntl(ArrRefMappingTypeForm)),
);
