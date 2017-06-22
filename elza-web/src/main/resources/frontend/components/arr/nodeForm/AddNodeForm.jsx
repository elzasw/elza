import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Modal, Button, Radio, FormGroup, ControlLabel, Form} from 'react-bootstrap';
import {WebApi} from 'actions/index.jsx';
import {addNode} from 'actions/arr/node.jsx';
import {AbstractReactComponent, i18n, FormInput, Loading} from 'components/index.jsx';
import {isFundRootId, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {indexById} from 'stores/app/utils.jsx'
import './AddNodeForm.less';
import {getSetFromIdsList} from "stores/app/utils.jsx";

/**
 * Dialog pro přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 26.8.2016
 */
class AddNodeForm extends AbstractReactComponent {

    static PropTypes = {
        node: React.PropTypes.object.isRequired,    // node pro který se akce volá
        parentNode: React.PropTypes.object.isRequired,  // nadřený node pro vstupní node
        initDirection: React.PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND']),
        nodeSettings: React.PropTypes.object.isRequired,
        onSubmit: React.PropTypes.func.isRequired,
        allowedDirections: React.PropTypes.arrayOf(React.PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND'])),
    };

    state = { // initial states
        scenarios: undefined,
        loading: false,
        selectedDirection: this.props.initDirection,
        selectedScenario: undefined,
        allowedDirections: ['BEFORE', 'AFTER', 'CHILD', 'ATEND']
    };

    /**
     * Připravuje hodnoty proměnných rodič, směr pro potřeby API serveru
     * - Přidání JP na konec se mění na přidání dítěte rodiči
     * @param {String} inDirection směr, kterým se má vytvořit nová JP
     * @param {Object} inNode uzel pro který je volána akce
     * @param {Object} inParentNode nadřazený uzel k inNode
     */
    formatDataForServer = (inDirection, inNode, inParentNode) => {
        let direction, node, parentNode;

        switch (inDirection) {
            case "ATEND":
                direction = "CHILD";
                node = inParentNode;
                parentNode = inParentNode;
            break;
            case "CHILD":
                direction = inDirection;
                node = inNode;
                parentNode = inNode;
            break;
            case "BEFORE":
            case "AFTER":
                direction = inDirection;
                node = inNode;
                parentNode = inParentNode;
            break;
        }

        return {direction: direction, activeNode: node, parentNode: parentNode}
    };

    /**
     * Načte scénáře a vrátí je pro zobrazení, v průběhu načístání nastaví ve state 'loading'
     * resp. uloží je do state 'scenarios'
     */
    getDirectionScenarios = (newDirection) => {
        if (newDirection === '') { // direction is not selected
            this.setState({
                scenarios: undefined
            });
            return;
        }
        // direction is selected
        this.setState({
            loading: true
        });
        const {node, parentNode, versionId} = this.props;
        // nastavi odpovidajiciho rodice a direction pro dotaz
        var dataServ = this.formatDataForServer(newDirection, node, parentNode)
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
    };

    handleDirectionChange = (e) => {
        var dr = e.target.value;
        this.setState({selectedDirection: dr});
        this.getDirectionScenarios(dr);
    };

    handleScenarioChange = (e) => {
        this.setState({selectedScenario: e.target.value});
    };

    /**
     * Vrátí prvky popisu ke zkopírování na základě proměnné props.nodeSettings
     */
    getDescItemTypeCopyIds = () => {
        const nodeSettings = this.props.nodeSettings;
        const nodeId = this.props.parentNode.id;

        let itemsToCopy = null;
        if (nodeSettings != undefined) {
            const nodeIndex = indexById(nodeSettings.nodes, nodeId);
            if (nodeIndex != null) {
                itemsToCopy = nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
            }
        }
        return itemsToCopy;
    };

    /**
     * Zprostředkuje přidání nové JP, dle aktuálně vyplněných dat ve formuláři
     * resp. dat uložených ve state 'selectedDirection', 'selectedScenario'
     */
    handleFormSubmit = (e) => {
        e.preventDefault();

        const {onSubmit, node, parentNode, versionId, initDirection} = this.props;
        const {selectedDirection, selectedScenario} = this.state;

        // nastavi odpovidajiciho rodice a direction pro dotaz
        const dataServ = this.formatDataForServer(selectedDirection, node, parentNode);

        // Data pro poslání do on submit - obsahují všechny informace pro založení
        const submitData = {
            indexNode: dataServ.activeNode,
            parentNode: dataServ.parentNode,
            versionId: versionId,
            direction: dataServ.direction,
            descItemCopyTypes: this.getDescItemTypeCopyIds(),
            scenarioName: selectedScenario
        };

        onSubmit(submitData);
    };

    componentWillMount() {
        const {initDirection} = this.props;
        this.getDirectionScenarios(initDirection);
    }

    render() {
        const {allowedDirections, onClose, node, parentNode, versionId, initDirection, arrRegion, userDetail} = this.props;
        const {scenarios, loading} = this.state;
        const notRoot = !isFundRootId(parentNode.id);

        var scnRadios= [];
        if(!loading) {
            var i = 0;
            if(scenarios) {
                for (i; i < scenarios.length; i++) {
                    scnRadios.push(<Radio key={'scns-' + i} defaultChecked={i === 0} autoFocus={i === 0} name='scns' onChange={this.handleScenarioChange} value={scenarios[i].name}>{scenarios[i].name}</Radio>);
                }
            }

            let strictMode = false;
            const fund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
            if (fund) {
                strictMode = fund.activeVersion.strictMode;

                let userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
                if (userStrictMode && userStrictMode.value !== null) {
                    strictMode = userStrictMode.value === 'true';
                }
            }

            if (!strictMode || i === 0) {
                scnRadios.push(<Radio key={'scns-' + i} defaultChecked={i === 0} autoFocus={i === 0} name='scns' onChange={this.handleScenarioChange} value={''}>{i18n('subNodeForm.add.noScenario')}</Radio>);
            }
        } else {
            scnRadios.push(<div>{i18n('arr.fund.addNode.noDirection')}</div>);
        }

        // Položky v select na směr
        const allowedDirectionsMap = getSetFromIdsList(allowedDirections);
        const directions = {
            "BEFORE":"before",
            "AFTER":"after",
            "CHILD":"child",
            "ATEND":"atEnd"
            };
        var options = [];
        for(let d in directions){
            if(allowedDirectionsMap[d]){
                if(isFundRootId(parentNode.id) && d !== "CHILD"){
                    continue;
                }
                options.push(<option value={d} key={d}>{i18n(`arr.fund.addNode.${directions[d]}`)}</option>);
            }
        }


        return <Form onSubmit={this.handleFormSubmit}>
            <Modal.Body>
                <FormInput ref="selsel" componentClass='select' disabled={loading} label={i18n('arr.fund.addNode.direction')} defaultValue={initDirection} onChange={this.handleDirectionChange}>
                    {options}
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
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" onClick={this.handleFormSubmit}>{i18n('global.action.store')}</Button>
                <Button bsStyle='link' onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>
    }
}

function mapStateToProps(state) {
    const {arrRegion, userDetail} = state;

    return {
        nodeSettings: arrRegion.nodeSettings,
        arrRegion: arrRegion,
        userDetail: userDetail
    }
}

export default connect(mapStateToProps)(AddNodeForm);
