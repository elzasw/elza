/**
 * Formulář Požadavku na digitaliyaci nebo skartaci/delimitaci.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {Field, reduxForm, formValueSelector} from 'redux-form';
import {connect} from 'react-redux';
import {AbstractReactComponent, FormInput} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';
import { messageFor } from 'components/shared/lang/dynamicMessage';
import { requestMessages, daoRequestTypeMessages } from './requestMessages';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {createDigitizationName} from './ArrUtils';
import {indexById} from 'stores/app/utils';
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import {refExternalSystemsFetchIfNeeded} from 'actions/refTables/externalSystems';
import FormInputField from '../shared/form/FormInputField';
import {ADMIN_USER, JAVA_ATTR_CLASS, JAVA_CLASS_ARR_DIGITIZATION_FRONTDESK_SIMPLE_VO} from '../../constants';

const DAO_TYPE = {
    DESTRUCTION: 'DESTRUCTION',
    TRANSFER: 'TRANSFER',
};
const DAO_TYPE_ALL = Object.keys(DAO_TYPE);

const ArrRequestForm = class extends AbstractReactComponent {
    static propTypes = {
        fundVersionId: PropTypes.number.isRequired,
        type: PropTypes.oneOf(['DAO', 'DIGITIZATION']),
    };

    static FORM = 'arrRequestForm';

    static fields = ['requestId', 'digitizationFrontdesk', 'daoType', 'description'];

    static validate = (values, props) => {
        const errors = {};
        if (props.type === 'DIGITIZATION') {
            if (values.digitizationFrontdesk === '') {
                errors.digitizationFrontdesk = getIntl().formatMessage(globalMessages.validationRequired);
            }
        }
        if (props.type === 'DAO') {
            if (values.daoType === '') {
                errors.daoType = getIntl().formatMessage(globalMessages.validationRequired);
            }
        }
        return errors;
    };

    componentDidMount() {
        this.props.dispatch(refExternalSystemsFetchIfNeeded());
        this.fetchDigitizationRequestList({}, this.props);
        this.trySelectRequest(this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(refExternalSystemsFetchIfNeeded());
        this.fetchDigitizationRequestList(this.props, nextProps);
    }

    trySelectRequest = props => {
        const {preparedRequestList, requestId, change} = props;
        if (preparedRequestList.rows.length > 0 && requestId === '') {
            let index = -1;
            for (let i = 0; i < preparedRequestList.rows.length; i++) {
                let row = preparedRequestList.rows[i];
                if (
                    props.userDetail.username === row.username ||
                    (props.userDetail.username === ADMIN_USER && row.username == null)
                ) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                const digReq = preparedRequestList.rows[index];
                change('requestId', digReq.id);
                change('description', digReq.description);
                change('digitizationFrontdesk', digReq.digitizationFrontdesk);
            }
        } else {
            change('requestId', null);
            change('description', null);
            const digitizationFrontdesks = this.getDigitizationFrontdesks();
            if (digitizationFrontdesks.length === 1) {
                change('digitizationFrontdesk', digitizationFrontdesks[0].id);
            } else {
                change('digitizationFrontdesk', null);
            }
        }
    };

    getDigitizationFrontdesks = () => {
        const {externalSystems} = this.props;
        const digitizationFrontdesks = [];
        if (externalSystems.fetched) {
            externalSystems.items.forEach(item => {
                if (item[JAVA_ATTR_CLASS] === JAVA_CLASS_ARR_DIGITIZATION_FRONTDESK_SIMPLE_VO) {
                    digitizationFrontdesks.push(item);
                }
            });
        }
        return digitizationFrontdesks;
    };

    fetchDigitizationRequestList = (prevProps, nextProps) => {
        this.props.dispatch(arrRequestActions.fetchPreparedListIfNeeded(nextProps.fundVersionId, nextProps.type));

        const requestFetched = nextProps.preparedRequestList.fetched && !nextProps.preparedRequestList.isFetching;
        const prevRequestFetched =
            prevProps.preparedRequestList &&
            prevProps.preparedRequestList.fetched &&
            !prevProps.preparedRequestList.isFetching;
        if (!prevRequestFetched && requestFetched) {
            // seznam byl načten, zkusíme vybrat jednu z možností, pokud není nic vybráno
            this.trySelectRequest(nextProps);
        }
    };

    handleDaoTypeChange = e => {
        const {change} = this.props;
        const value = e.target.value;
        change('requestId', null);
        change('description', '');
        this.props.dispatch(arrRequestActions.filterPreparedList({daoType: value}));
        this.trySelectRequest(this.props);
    };

    handleRequestChange = e => {
        const {preparedRequestList, change} = this.props;
        const id = e.target.value;

        change('requestId', e);

        if (id !== '') {
            // vybrána nějaká možnost, převezmeme z ní popis
            const digReq = preparedRequestList.rows[indexById(preparedRequestList.rows, id)];
            change('digitizationFrontdesk', digReq.digitizationFrontdeskId);
            change('description', digReq.description);
        } else {
            // vybrána možnost nového požadavku, vynulujeme popis
            change('description', '');
            const digitizationFrontdesks = this.getDigitizationFrontdesks();
            if (digitizationFrontdesks.length === 1) {
                change('digitizationFrontdesk', digitizationFrontdesks[0].id);
            } else {
                change('digitizationFrontdesk', null);
            }
        }
    };

    render() {
        const {
            type,
            handleSubmit,
            onSubmitForm,
            userDetail,
            preparedRequestList,
            onClose,
            externalSystems,
            daoType,
        } = this.props;
        const requestFetched = preparedRequestList.fetched && !preparedRequestList.isFetching;

        const showDaoTypeSelect = type === 'DAO';
        const showDigitizationFrontdeskSelect = type === 'DIGITIZATION';
        const showRequestFields = (type === 'DAO' && daoType) || type !== 'DAO';

        let digitizationFrontdesks = [];

        if (showDigitizationFrontdeskSelect) {
            digitizationFrontdesks = this.getDigitizationFrontdesks();
        }

        return (
            <Form onSubmit={handleSubmit(onSubmitForm)}>
                <Modal.Body>
                    {showDaoTypeSelect && (
                        <Field
                            component={FormInputField}
                            label={<FormattedMessage {...requestMessages.requestTitleDaoRequestType} />}
                            type="select"
                            onChange={this.handleDaoTypeChange}
                            name={'daoType'}
                        >
                            <option key={-1} value=""></option>
                            {DAO_TYPE_ALL.map(i => (
                                <option key={i} value={i}>
                                    {this.props.intl.formatMessage(messageFor(daoRequestTypeMessages, i, requestMessages.requestTitleTypeDaoTRANSFER))}
                                </option>
                            ))}
                        </Field>
                    )}
                    {showRequestFields && (
                        <Field
                            component={FormInputField}
                            label={<FormattedMessage {...requestMessages.requestTitleDigitizationRequest} />}
                            type="select"
                            name={'requestId'}
                            onChange={this.handleRequestChange}
                            disabled={!requestFetched}
                        >
                            <option key={-1} value={null}>
                                {<FormattedMessage {...requestMessages.requestTitleNewRequest} />}
                            </option>
                            {preparedRequestList.fetched &&
                                !preparedRequestList.isFetching &&
                                preparedRequestList.rows.map(digReq => (
                                    <option value={digReq.id} key={digReq.id}>
                                        {createDigitizationName(digReq, userDetail)}
                                    </option>
                                ))}
                        </Field>
                    )}
                    {showDigitizationFrontdeskSelect && (
                        <Field
                            component={FormInputField}
                            label={<FormattedMessage {...requestMessages.requestTitleDaoRequestDigitizationFrontdesk} />}
                            type="select"
                            name={'digitizationFrontdesk'}
                            disabled={!requestFetched}
                        >
                            <option key={-1} value=""></option>
                            {digitizationFrontdesks.map(digFrontdesk => (
                                <option value={digFrontdesk.id} key={digFrontdesk.id}>
                                    {digFrontdesk.name}
                                </option>
                            ))}
                        </Field>
                    )}
                    {showRequestFields && (
                        <Field
                            component={FormInputField}
                            label={<FormattedMessage {...requestMessages.requestTitleDescription} />}
                            type="textarea"
                            name={'description'}
                            disabled={!requestFetched}
                        />
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        type="submit"
                        variant="outline-secondary"
                        onClick={handleSubmit(onSubmitForm.bind(this, false))}
                    >
                        {<FormattedMessage {...globalMessages.save} />}
                    </Button>
                    <Button
                        type="submit"
                        variant="outline-secondary"
                        onClick={handleSubmit(onSubmitForm.bind(this, true))}
                    >
                        {<FormattedMessage {...requestMessages.requestActionStoreAndSend} />}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {<FormattedMessage {...globalMessages.close} />}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
};

const selector = formValueSelector(ArrRequestForm.FORM);

function mapStateToProps(state) {
    const {arrRegion, refTables, userDetail} = state;

    return {
        funds: arrRegion.funds,
        externalSystems: refTables.externalSystems,
        userDetail,
        preparedRequestList: state.app.preparedRequestList,
        initialValues: {daoType: null, requestId: null, description: null},
        daoType: selector(state, 'daoType'),
        requestId: selector(state, 'requestId'),
    };
}

const form = reduxForm({
    form: ArrRequestForm.FORM,
    validate: ArrRequestForm.validate,
})(injectIntl(ArrRequestForm));

export default connect(mapStateToProps)(form);
