import React from 'react';
import {Button, Form, Modal, Checkbox} from "react-bootstrap";
import {i18n, AbstractReactComponent, FormInput} from "components/shared";
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
        descItemFactory: PropTypes.object.isRequired
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

    customRender = (code, infoType) => {
        if (code === 'INT') {
            const {fields: {itemTypeIds}} = this.props;
            const index = itemTypeIds.value.indexOf(infoType.id);
            const checked = index !== -1;


            return <Checkbox key="increment" checked={checked} onChange={() => {
                if (checked) {
                    itemTypeIds.onChange([
                        ...itemTypeIds.value.slice(0, index),
                        ...itemTypeIds.value.slice(index+1),
                    ]);
                } else {
                    itemTypeIds.onChange([
                        ...itemTypeIds.value,
                        infoType.id
                    ]);
                }

            }}>
                {i18n('arr.structure.modal.increment')}
            </Checkbox>;
        }
        return null;
    };

    render() {
        const {fields: {count, itemTypeIds}, error, handleSubmit, onClose, submitting, structureData, fundVersionId, fundId, multiple} = this.props;

        return <Form onSubmit={handleSubmit}>
            <Modal.Body>
                {error && <p>{error}</p>}
                <StructureSubNodeForm versionId={fundVersionId}
                                      fundId={fundId}
                                      selectedSubNodeId={structureData.id}
                                      customActions={multiple && this.customRender} // pokud form je mnohonásobný renderujeme doplňkově inkrementaci
                                      x={itemTypeIds} // Zdůvodu renderování formu aby při změně nastal render
                                      descItemFactory={this.props.descItemFactory}
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


export default reduxForm({
    form: 'AddStructureData',
    initialValues: {count: "", itemTypeIds: []},
    fields: ['count', 'itemTypeIds'],
    validate: (data, props) => {
        const errors = {};
        if (props.multiple) {
            if (!data.count || data.count < 2) {
                errors.count = i18n("arr.structure.modal.addMultiple.error.count.tooSmall");
            }
            if (!data.itemTypeIds || data.itemTypeIds.length < 1) {
                errors._error = i18n("arr.structure.modal.addMultiple.error.itemTypeIds.required");
            }
        }
        return errors;
    }
})(AddStructureDataForm);
