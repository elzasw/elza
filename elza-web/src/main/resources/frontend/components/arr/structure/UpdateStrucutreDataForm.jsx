import React from 'react';
import {connect} from 'react-redux';
import {Button, Modal} from "react-bootstrap";
import {i18n, AbstractReactComponent} from "components";
import StructureSubNodeForm from "./StructureSubNodeForm";
import {structureNodeFormFetchIfNeeded} from "../../../actions/arr/structureNodeForm";
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
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    componentWillReceiveProps(nextProps) {
        const {fundVersionId, id} = nextProps;
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    render() {
        const {onClose, fundVersionId, fundId, structureNodeForm, readMode, descItemFactory} = this.props;

        return <div>
            <Modal.Body>
            {structureNodeForm.fetched ?
                <StructureSubNodeForm
                    versionId={fundVersionId}
                    readMode={readMode}
                    fundId={fundId}
                    selectedSubNodeId={structureNodeForm.id}
                    descItemFactory={descItemFactory}
                />:
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
    const {arrRegion} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    return {
        structureNodeForm: fund ? fund.structureNodeForm : null
    }
})(UpdateStructureDataForm);
