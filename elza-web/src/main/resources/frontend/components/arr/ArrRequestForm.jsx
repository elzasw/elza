/**
 * Formulář Požadavku na digitaliyaci nebo skartaci/delimitaci.
 */

import React from 'react';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button, Input, Form} from 'react-bootstrap';
import {createDigitizationName} from './ArrUtils.jsx'
import {indexById} from 'stores/app/utils.jsx';
import * as arrRequestActions from "actions/arr/arrRequestActions";

const ArrRequestForm = class extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    static PropTypes = {
        fundVersionId: React.PropTypes.number.isRequired,
        type: React.PropTypes.oneOf(["DAO", "DIGITIZATION"]),
    };

    static fields = [
        'digitizationRequestId',
        'description',
    ];

    componentDidMount() {
        this.fetchDigitizationRequestList({}, this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.fetchDigitizationRequestList(this.props, nextProps);
    }

    fetchDigitizationRequestList = (prevProps, nextProps) => {
        this.props.dispatch(arrRequestActions.fetchPreparedListIfNeeded(nextProps.fundVersionId, nextProps.type));

        const digitizationRequestFetched = nextProps.preparedRequestList.fetched && !nextProps.preparedRequestList.isFetching;
        const prevDigitizationRequestFetched = prevProps.preparedRequestList &&  prevProps.preparedRequestList.fetched && !prevProps.preparedRequestList.isFetching;
        if (!prevDigitizationRequestFetched && digitizationRequestFetched) {    // seznam byl načten, zkusíme vybrat jednu z možností, pokud není nic vybráno
            const {fields: {digitizationRequestId, description}} = nextProps;
            if (nextProps.preparedRequestList.rows.length > 0 && digitizationRequestId.value === "") {
                let index = -1;
                for (let i = 0; i < nextProps.preparedRequestList.rows.length; i++) {
                    let row = nextProps.preparedRequestList.rows[i];
                    if (nextProps.userDetail.username == row.username || (nextProps.userDetail.username == 'admin' && row.username == null)) {
                        index = i;
                        break;
                    }
                }
                if (index >= 0) {
                    digitizationRequestId.onChange(nextProps.preparedRequestList.rows[index].id);
                    description.onChange(nextProps.preparedRequestList.rows[index].description);
                }
            } else {
                description.onChange("");
            }
        }
    }

    handleRequestChange = (e) => {
        const {preparedRequestList, fields: {digitizationRequestId, description}} = this.props;
        const id = e.target.value;

        if (id !== "") {  // vybrána nějaká možnost, převezmeme z ní popis
            const digReq = preparedRequestList.rows[indexById(preparedRequestList.rows, id)];
            digitizationRequestId.onChange(e);
            description.onChange(digReq.description);
        } else {    // vybrána možnost nového požadavku, vynulujeme popis
            digitizationRequestId.onChange(e);
            description.onChange("");
        }
    }

    render() {
        const {handleSubmit, onSubmitForm, userDetail, preparedRequestList, onClose, fields: {digitizationRequestId, description}} = this.props;
        const digitizationRequestFetched = preparedRequestList.fetched && !preparedRequestList.isFetching;

        const form = (
            <div>
                <FormInput label={i18n("arr.request.title.digitizationRequest")} componentClass="select" {...digitizationRequestId} onChange={this.handleRequestChange} disabled={!digitizationRequestFetched}>
                    <option key={-1} value="">{i18n('digitizationRequest.title.newRequest')}</option>
                    {preparedRequestList.fetched
                    && !preparedRequestList.isFetching
                    && preparedRequestList.rows.map(digReq => <option value={digReq.id}
                                                                          key={digReq.id}>{createDigitizationName(digReq, userDetail)}</option>)}
                </FormInput>
                <FormInput label={i18n("arr.request.title.description")} componentClass="textarea" {...description} disabled={!digitizationRequestFetched} />
            </div>
        )

        return (
            <Form onSubmit={handleSubmit(onSubmitForm)}>
                <Modal.Body>
                    {form}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" onClick={handleSubmit(onSubmitForm.bind(this, false))}>{i18n('global.action.store')}</Button>
                    <Button type="submit" onClick={handleSubmit(onSubmitForm.bind(this, true))}>{i18n('arr.request.action.storeAndSend')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </Form>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, refTables, userDetail} = state
    return {
        funds: arrRegion.funds,
        refTables,
        packets: arrRegion.packets,
        userDetail,
        preparedRequestList: state.app.preparedRequestList
    }
}

export default reduxForm({
        form: 'arrRequestForm',
        fields: ArrRequestForm.fields,
    }, mapStateToProps
)(ArrRequestForm)
