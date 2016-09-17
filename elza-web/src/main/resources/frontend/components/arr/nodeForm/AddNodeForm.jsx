/**
 * Dialog pro přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 26.8.2016
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Modal, Button, Radio, FormGroup, ControlLabel} from 'react-bootstrap';
import {WebApi} from 'actions/index.jsx';
import {addNode} from 'actions/arr/node.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {AbstractReactComponent, i18n, FormInput, Loading} from 'components/index.jsx';
import {isFundRootId} from 'components/arr/ArrUtils.jsx'
import {indexById} from 'stores/app/utils.jsx'

require ('./AddNodeForm.less');

var AddNodeForm = class AddNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('formatDataForServer', 'handleDirectionChange', 'handleScenarioChange', 'getDirectionScenarios', 'handleFormSubmit',
            'getDescItemTypeCopyIds');
        this.state = { // initial states
            scenarios: undefined,
            loading: false,
            selectedDirection: props.initDirection,
            selectedScenario: undefined
        };
    }

    /**
     * Připravuje hodnoty proměnných rodič, směr pro potřeby API serveru
     * - Přidání JP na konec se mění na přidání dítěte rodiči
     * @param {String} inDirection směr, kterým se má vytvořit nová JP
     * @param {Object} inNode uzel pro který je volána akce
     */
    formatDataForServer(inDirection, inNode) {
        var di, no, pno;
        if(inDirection == 'ATEND') { // prvek na konec seznamu
            di = 'CHILD';
            no = inNode;
            pno = inNode;
        } else {
            di = inDirection;
            no = inNode.subNodeForm.data.parent;
            pno = inNode;
            if(inDirection == 'CHILD') {
                pno = inNode.subNodeForm.data.parent;
            }
        }
        return {direction: di, activeNode: no, parentNode: pno}
    }

    /**
     * Načte scénáře a vrátí je pro zobrazení, v průběhu načístání nastaví ve state 'loading'
     * resp. uloží je do state 'scenarios'
     */
    getDirectionScenarios(newDirection) {
        if(newDirection === '') { // direction is not selected
            this.setState({
                scenarios: undefined
            });
            return;
        }
        // direction is selected
        this.setState({
            loading: true
        });
        const {node, versionId} = this.props;
        // nastavi odpovidajiciho rodice a direction pro dotaz
        var dataServ = this.formatDataForServer(newDirection, node)
        // ajax dotaz na scenare
        WebApi.getNodeAddScenarios(dataServ.activeNode, versionId, dataServ.direction).then(
            (result) => { // resolved
                // select first scenario
                var selScn = undefined;
                if(result.length > 0) selScn = result[0].name;
                this.setState({
                    scenarios: result,
                    loading: false,
                    selectedScenario: selScn
                });
            },
            (reason) => { // rejected
                this.setState({
                    scenarios: undefined,
                    loading: false,
                    selectedScenario: undefined
                });
            }
        );
    }

    handleDirectionChange(e) {
        var dr = e.target.value;
        this.setState({selectedDirection: dr});
        this.getDirectionScenarios(dr);
    }

    handleScenarioChange(e) {
        this.setState({selectedScenario: e.target.value});
    }

    /**
     * Vrátí prvky popisu ke zkopírování na základě proměnné props.nodeSettings
     */
    getDescItemTypeCopyIds() {
        const nodeSettings = this.props.nodeSettings;
        const nodeId = this.props.node.id;

        let itemsToCopy = null;
        if (nodeSettings != undefined) {
            const nodeIndex = indexById(nodeSettings.nodes, nodeId);
            if (nodeIndex != null) {
                itemsToCopy = nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
            }
        }
        return itemsToCopy;
    }

    /**
     * Zprostředkuje přidání nové JP, dle aktuálně vyplněných dat ve formuláři
     * resp. dat uložených ve state 'selectedDirection', 'selectedScenario'
     */
    handleFormSubmit(e) {
        e.preventDefault();
        const {node, versionId, initDirection, handlePostSubmitActions} = this.props;
        var selDi = this.state.selectedDirection;
        var selScn = this.state.selectedScenario;
        // nastavi odpovidajiciho rodice a direction pro dotaz
        var dataServ = this.formatDataForServer(selDi, node);
        this.dispatch(addNode(dataServ.activeNode, dataServ.parentNode, this.props.versionId, dataServ.direction, this.getDescItemTypeCopyIds(), selScn));
        handlePostSubmitActions();
    }

    componentWillMount() {
        const {initDirection} = this.props;
        this.getDirectionScenarios(initDirection);
    }

    render() {
        const {onClose, node, versionId, initDirection} = this.props;
        const {scenarios, loading} = this.state;
        const notRoot = !isFundRootId(node.id);

        var scnRadios= [];
        if(!loading) {
            var i = 0;
            if(scenarios) {
                for (i; i < scenarios.length; i++) {
                    scnRadios.push(<Radio key={'scns-' + i} defaultChecked={i === 0} autoFocus={i === 0} name='scns' onChange={this.handleScenarioChange} value={scenarios[i].name}>{scenarios[i].name}</Radio>);
                }
            }
            scnRadios.push(<Radio key={'scns-' + i} defaultChecked={i === 0} autoFocus={i === 0} name='scns' onChange={this.handleScenarioChange} value={''}>{i18n('subNodeForm.add.noScenario')}</Radio>);
        } else {
            scnRadios.push(<div>{i18n('arr.fund.addNode.noDirection')}</div>);
        }

        return(
            <div>
                <Modal.Body>
                    <form onSubmit={this.handleFormSubmit}>
                        <FormInput ref="selsel" componentClass='select' disabled={loading} label={i18n('arr.fund.addNode.direction')} defaultValue={initDirection} onChange={this.handleDirectionChange}>
                            <option value=''/>
                            {notRoot && [
                                <option value='BEFORE' key='BEFORE'>{i18n('arr.fund.addNode.before')}</option>,
                                <option value='AFTER' key='AFTER'>{i18n('arr.fund.addNode.after')}</option>
                            ]}
                            <option value='CHILD' key='CHILD'>{i18n('arr.fund.addNode.child')}</option>
                            {notRoot && [
                                <option value='ATEND' key='ATEND'>{i18n('arr.fund.addNode.atEnd')}</option>
                            ]}
                        </FormInput>
                        <FormGroup>
                            <ControlLabel>{i18n('arr.fund.addNode.scenario')}</ControlLabel>
                            {loading ? <Loading /> :
                                <FormGroup key='Scenarios'>
                                    {scnRadios}
                                </FormGroup>
                            }
                        </FormGroup>
                        <Button type='submit' className='hide'/>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.handleFormSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle='link' onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        );
    }
};

function mapStateToProps(state) {
    const {arrRegion} = state

    return {
        nodeSettings: arrRegion.nodeSettings,
    }
}

AddNodeForm.propTypes = {
    node: React.PropTypes.object.isRequired,
    initDirection: React.PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND', '']),
    nodeSettings: React.PropTypes.object.isRequired
};

module.exports = connect(mapStateToProps)(AddNodeForm);
