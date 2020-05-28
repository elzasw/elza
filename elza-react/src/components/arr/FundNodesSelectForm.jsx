import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Modal} from 'react-bootstrap';
import {Button} from '../ui';
import FundNodesSelect from './FundNodesSelect';
import {customFundActionFetchListIfNeeded} from '../../actions/arr/customFundAction';
import * as types from '../../actions/constants/ActionTypes.js';
import Loading from '../shared/loading/Loading';
import PropTypes from 'prop-types';

/**
 * Dialogový formulář vybrání uzlů v konkrétní verzi souboru - výběr uzlů na základě konfigurace - např. single nebo multiple select.
 * Implicitně pokud se neuvede, je výběr multiselect libovolných položek ve stromu.
 */
class FundNodesSelectForm extends AbstractReactComponent {
    static propTypes = {
        multipleSelection: PropTypes.bool,
        multipleSelectionOneLevel: PropTypes.bool,
    };

    static defaultProps = {
        multipleSelection: true,
        multipleSelectionOneLevel: false,
        fundId: null,
    };

    state = {
        selectedNodes: [],
        selectedNodesIds: [],
    };

    componentDidMount() {
        this.fetch(this.props);
    }

    fetch = props => {
        if (props.fundId) {
            props.dispatch(customFundActionFetchListIfNeeded(props.fundId));
        }
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
            selectedNodesIds: ids,
        });
    };

    render() {
        const {multipleSelection, multipleSelectionOneLevel, onClose, fund, fundId} = this.props;
        const {selectedNodes} = this.state;

        let someSelected = selectedNodes.length > 0;

        if (fund && fund.fundId !== fundId) {
            return <Loading />;
        }

        return (
            <div className="add-nodes-form-container">
                <Modal.Body>
                    <FundNodesSelect
                        multipleSelection={multipleSelection}
                        multipleSelectionOneLevel={multipleSelectionOneLevel}
                        onChange={this.handleChange}
                        fund={fund}
                        area={fund ? types.CUSTOM_FUND_TREE_AREA_NODES : undefined}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button disabled={!someSelected} variant={"outline-secondary"} onClick={this.handleSubmit}>
                        {i18n('global.action.select')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
}

function mapStateToProps(state, props) {
    const {arrRegion} = state;
    return {
        fund: props.fundId ? arrRegion.customFund : null,
    };
}

export default connect(mapStateToProps)(FundNodesSelectForm);
