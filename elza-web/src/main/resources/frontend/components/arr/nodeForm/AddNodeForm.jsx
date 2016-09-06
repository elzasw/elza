/**
 * Dialog pro přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 26.8.2016
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import * as types from 'actions/constants/ActionTypes.js';
import {AbstractReactComponent, i18n, FormInput, Loading} from 'components/index.jsx';
import {Modal, Button, Radio, FormGroup, ControlLabel} from 'react-bootstrap';
import {WebApi} from 'actions/index.jsx';

require ('./AddNodeForm.less');

var AddNodeForm = class AddNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleDirectionChange');
        this.state = { // initial states
            scenarios: undefined,
            loading: false
        };
    }

    /**
     * Načte scénáře a vrátí je pro zobrazení
     */
    handleDirectionChange(e) {
        var newDirection = e.target.value;
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
        let no, di;
        if(newDirection == 'ATEND') { // u prvku na konec seznamu
            no = node;
            di = 'CHILD';
        } else {
            no = node.subNodeForm.data.parent;
            di = newDirection;
        }
        // ajax dotaz na scenare
        WebApi.getNodeAddScenarios(no, versionId, di).then(
            (result) => { // resolved
                this.setState({
                    scenarios: result,
                    loading: false
                });
            },
            (reason) => { // rejected
                this.setState({
                    scenarios: undefined,
                    loading: false
                });
            }
        );
    }

    componentDidMount() {
        
    }

    render() {
        const {handleSubmit, onClose, node, versionId, initDirection} = this.props;
        const {scenarios, loading} = this.state;

        var scnRadios= [];
        if(scenarios) {
            for (var i = 0; i < scenarios.length; i++) {
                scnRadios.push(<Radio key={'scns-' + i} name='scns' value={scenarios[i].name}>{scenarios[i].name}</Radio>);
            }
        } else {
            scnRadios.push(<div>{i18n('arr.fund.addNode.noDirection')}</div>);
        }

        return(
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <FormInput componentClass='select' disabled={loading} label={i18n('arr.fund.addNode.direction')} defaultValue={initDirection} onChange={this.handleDirectionChange}>
                            <option value=''/>
                            <option value='BEFORE' key='BEFORE'>{i18n('arr.fund.addNode.before')}</option>
                            <option value='AFTER' key='AFTER'>{i18n('arr.fund.addNode.after')}</option>
                            <option value='CHILD' key='CHILD'>{i18n('arr.fund.addNode.child')}</option>
                            <option value='ATEND' key='ATEND'>{i18n('arr.fund.addNode.atEnd')}</option>
                        </FormInput>
                        <FormGroup>
                            <ControlLabel>{i18n('arr.fund.addNode.scenario')}</ControlLabel>
                            {loading ? <Loading /> :
                                <FormGroup key='Scenarios'>
                                    {scnRadios}
                                </FormGroup>
                            }
                        </FormGroup>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        );
    }
};

AddNodeForm.propTypes = {
    node: React.PropTypes.object.isRequired,
    initDirection: React.PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND', ''])
};

module.exports = connect()(AddNodeForm);
