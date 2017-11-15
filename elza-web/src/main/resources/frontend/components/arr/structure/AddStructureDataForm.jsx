import React from 'react';
import {Button, Form, Modal} from "react-bootstrap";
import {i18n, AbstractReactComponent, FormInput} from "components";
import {connect} from "react-redux";
import {reduxForm} from "redux-form";
import StructureSubNodeForm from "./StructureSubNodeForm";
import {structureNodeFormSelectId} from "../../../actions/arr/structureNodeForm";
import PropTypes from 'prop-types';

class AddStructureDataForm extends AbstractReactComponent {

    static propTypes = {
        multiple: PropTypes.bool,
        fundVersionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        structureData: PropTypes.object.isRequired,
    };

    static defaultProps = {
        multiple: false,
    };

    componentWillMount() {
        const {fundVersionId, structureData} = this.props;
        this.props.dispatch(structureNodeFormSelectId(fundVersionId, structureData.id));
    }

    componentWillReceiveProps(nextProps) {
        const {fundVersionId, structureData: {id}} = nextProps;
        if (id !== this.props.structureData.id) {
            this.props.dispatch(structureNodeFormSelectId(fundVersionId, id));
        }
    }

    // TODO implement itemTypeIds search for multiple creation
    // submit = (data) => {
    //
    //     return this.props.onSubmit({...data, itemTypeIds: []});
    // };

    render() {
        const {fields: {count}, handleSubmit, onClose, submitting, structureData, fundVersionId, fundId, multiple} = this.props;

        return <Form onSubmit={handleSubmit}>
            <Modal.Body>
                <StructureSubNodeForm versionId={fundVersionId}
                                      fundId={fundId}
                                      selectedSubNodeId={structureData.id}
                />
                {multiple && <FormInput name="count" type="number" label={i18n("arr.structure.modal.addMultiple.count")} {...count} />}
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('global.action.add')}</Button>
                <Button bsStyle="link" disabled={submitting} onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}


export default connect()(reduxForm({
    form: 'AddStructureData',
    initialValues: {count: ""},
    fields: ['count'],
    validate: (data, props) => {
        const errors = {};
        if (props.multiple) {
            if (!data.count || data.count < 2) {
                errors.count = i18n("arr.structure.modal.addMultiple.error.count.tooSmall");
            }
        }

        return errors;
    }
})(AddStructureDataForm));
