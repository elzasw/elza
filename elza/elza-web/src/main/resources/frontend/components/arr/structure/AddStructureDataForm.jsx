import React from 'react';
import {Button, Form, Modal, Checkbox} from "react-bootstrap";
import {i18n, AbstractReactComponent, FormInput} from "components/shared";
import {connect} from "react-redux";
import {reduxForm} from "redux-form";
import StructureSubNodeForm from "./StructureSubNodeForm";
import {structureNodeFormSelectId, structureNodeFormFetchIfNeeded} from "../../../actions/arr/structureNodeForm";
import PropTypes from 'prop-types';
import objectById from "../../../shared/utils/objectById";

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
        const {structureData, fundVersionId} = this.props;
        this.props.dispatch(structureNodeFormSelectId(structureData.id));
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, structureData.id));
    }

    componentWillReceiveProps(nextProps) {
        const {structureData: {id}, fundVersionId} = nextProps;
        if (id !== this.props.structureData.id) {
            this.props.dispatch(structureNodeFormSelectId(id));
        }
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    customRender = (code, infoType) => {
        if (code === 'INT') {
            const {fields: {incrementedTypeIds}} = this.props;
            const index = incrementedTypeIds.value.indexOf(infoType.id);
            const checked = index !== -1;


            return <Checkbox key="increment" checked={checked} onChange={() => {
                if (checked) {
                    incrementedTypeIds.onChange([
                        ...incrementedTypeIds.value.slice(0, index),
                        ...incrementedTypeIds.value.slice(index + 1),
                    ]);
                } else {
                    incrementedTypeIds.onChange([
                        ...incrementedTypeIds.value,
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
        const {fields: {count, incrementedTypeIds}, error, handleSubmit, onClose, submitting, structureData, fundVersionId, fundId, multiple, storeStructure} = this.props;

        return <Form onSubmit={handleSubmit}>
            <Modal.Body>
                {error && <p>{error}</p>}
                {storeStructure && storeStructure.fetched && <StructureSubNodeForm
                    id={structureData.id}
                    versionId={fundVersionId}
                    fundId={fundId}
                    selectedSubNodeId={structureData.id}
                    customActions={multiple && this.customRender} // pokud form je mnohonásobný renderujeme doplňkově inkrementaci
                    // Pyta: Jak toto funguje, neni to tu nadbytecne?
                    x={incrementedTypeIds} // Zdůvodu renderování formu aby při změně nastal render
                    descItemFactory={this.props.descItemFactory}
                />}
                {multiple && <FormInput name="count" min="2" type="number"
                                        label={i18n("arr.structure.modal.addMultiple.count")} {...count} />}
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('global.action.add')}</Button>
                <Button bsStyle="link" disabled={submitting} onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

const form = reduxForm({
    form: 'AddStructureData',
    fields: [
        // count of created items
        'count',
        //  IDs of item types which will be inceremented
        'incrementedTypeIds'
    ],
    initialValues: {
        count: "",
        incrementedTypeIds: []
    },
    validate: (values, props) => {
        const errors = {};
        if (props.multiple) {
            if (!values.count || values.count < 2) {
                errors.count = i18n("arr.structure.modal.addMultiple.error.count.tooSmall");
            }
            if (!values.incrementedTypeIds || values.incrementedTypeIds.length < 1) {
                errors._error = i18n("arr.structure.modal.addMultiple.error.itemTypeIds.required");
            }
        }
        return errors;
    }
})(AddStructureDataForm);


export default connect((state, props) => {
    const {structures} = state;

    return {
        storeStructure: props.structureData && structures.stores.hasOwnProperty(props.structureData.id) ? structures.stores[props.structureData.id] : null,
    }
})(form);
