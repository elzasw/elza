/**
 *  Komponenta pro vyhledávání
 *
 *  Pro inicializaci staci naimportovat: import {Search} from 'components'
 *
 **/

import React from 'react';

import {Button, Input} from 'react-bootstrap';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from 'components';
import ReactDOM from 'react-dom'
import {requestScopesIfNeeded} from 'actions/scopes/scopesData'
require ('./Scope.less');


/**
 *  Komponenta pro scope
 *  @param versionId zadat null nebo id verze
 *  <Scope versionId={null} label='Scope'/>
 **/
var Scope = class Scope extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleChange');
        this.dispatch(requestScopesIfNeeded(this.props.versionId));
        this.state = {
            value: props.value,
        }
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(requestScopesIfNeeded(nextProps.versionId));
        this.state = {
            value: nextProps.value,
        }
    }

    handleChange( e) {
        this.setState({
            value: e.target.value
        });
        if (this.props.onSelect){
            this.props.onSelect(e.target.value);
        }

    }

    render() {
        const {value} = this.props;

        var inputLabel;
        if (this.props.label) {
            inputLabel = <label className='control-label'><span>{this.props.label}</span></label>
        }

        var data = [];
        this.props.refTables.scopesData.scopes.map(scope => {
                if (scope.versionId === this.props.versionId) {
                    data = scope.scopes.data;
                }
            }
        );

        return (
            <div>
                {inputLabel}
                <Input type='select' options={data} onChange={this.handleChange}>
                    <option value="0" key="0"></option>
                    {data.map((i)=> {return <option value={i.id} key={i.id}>{i.name}</option>})}
                </Input>
            </div>
        );
    }
}

Scope.propTypes = {
    value: React.PropTypes.number,

}

function mapStateToProps(state) {
    const {refTables} = state
    return {
        refTables
    }
}

module.exports = connect(mapStateToProps)(Scope);