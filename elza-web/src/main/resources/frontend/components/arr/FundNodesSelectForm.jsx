import React from 'react';
import {connect} from 'react-redux'
import {i18n, FundTreeLazy, AbstractReactComponent} from 'components/index.jsx';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Modal, Button, Input, Form} from 'react-bootstrap';
import {
    fundTreeFulltextChange,
    fundTreeFulltextSearch,
    fundTreeFulltextNextItem,
    fundTreeFulltextPrevItem,
    fundTreeSelectNode,
    fundTreeCollapse,
    fundTreeFocusNode,
    fundTreeFetchIfNeeded,
    fundTreeNodeExpand,
    fundTreeNodeCollapse,
    fundTreeConfigure
} from 'actions/arr/fundTree.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'
import FundNodesSelect from "./FundNodesSelect";

/**
 * Dialogový formulář vybrání uzlů v konkrétní verzi souboru - výběr uzlů na základě konfigurace - např. single nebo multiple select.
 * Implicitně pokud se neuvede, je výběr multiselect libovolných položek ve stromu.
 */
class FundNodesSelectForm extends AbstractReactComponent {

    static propTypes = {
        ...FundNodesSelect.propTypes
    };

    static defaultProps = {
        multipleSelection: true,
        multipleSelectionOneLevel: false,
    };

    state = {
        selectedNodes: [],
        selectedNodesIds: []
    };

    handleSubmit = () => {
        const {multipleSelection, onSubmitForm} = this.props;
        const {selectedNodes, selectedNodesIds} = this.state;

        if (multipleSelection) {
            onSubmitForm(selectedNodesIds, selectedNodes);
        } else {
            onSubmitForm(selectedNodesIds[0], selectedNodes[0]);
        }
    };

    handleChange = (ids, nodes) => {
        this.setState({
            selectedNodes: nodes,
            selectedNodesIds: ids
        });
    }

    render() {
        const {multipleSelection, multipleSelectionOneLevel, onClose} = this.props;
        const {selectedNodes} = this.state;

        let someSelected = selectedNodes.length > 0;

        return (
            <div className="add-nodes-form-container">
                <Modal.Body>
                    <FundNodesSelect
                        multipleSelection={multipleSelection}
                        multipleSelectionOneLevel={multipleSelectionOneLevel}
                        onChange={this.handleChange}
                        />
                </Modal.Body>
                <Modal.Footer>
                    <Button disabled={!someSelected} onClick={this.handleSubmit}>{i18n('global.action.select')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default connect()(FundNodesSelectForm);

