import React from 'react';
import {connect} from 'react-redux';
import {Button, Modal} from "react-bootstrap";
import {i18n, AbstractReactComponent} from "components";
import StructureSubNodeForm from "./StructureSubNodeForm";
import {structureNodeFormFetchIfNeeded, structureNodeFormSelectId} from "../../../actions/arr/structureNodeForm";
import Loading from "../../shared/loading/Loading";
import PropTypes from 'prop-types';

class UpdateStructureDataForm extends AbstractReactComponent {

    static propTypes = {
        multiple: PropTypes.bool,
        fundVersionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        id: PropTypes.number.isRequired,
        readMode: PropTypes.bool.isRequired,
        descItemFactory: PropTypes.object.isRequired
    };

    componentWillMount() {
        const {fundVersionId, id} = this.props;
        this.props.dispatch(structureNodeFormSelectId(id));
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    componentWillReceiveProps(nextProps) {
        const {fundVersionId, id} = nextProps;
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    render() {
        const {onClose, fundVersionId, fundId, structureNodeForm, readMode, descItemFactory, id} = this.props;

        return <div>
            <Modal.Body>
            {structureNodeForm && structureNodeForm.fetched ?
                <StructureSubNodeForm
                    id={id}
                    versionId={fundVersionId}
                    readMode={readMode}
                    fundId={fundId}
                    selectedSubNodeId={structureNodeForm.id}
                    descItemFactory={descItemFactory}
                /> :
                <Loading />
            }
            </Modal.Body>
            <Modal.Footer>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
            </Modal.Footer>
        </div>
    }
}

export default connect((state, props) => {
    const {structures} = state;
    return {
        structureNodeForm: structures.stores.hasOwnProperty(props.id) ? structures.stores[props.id] : null
    }
})(UpdateStructureDataForm);
