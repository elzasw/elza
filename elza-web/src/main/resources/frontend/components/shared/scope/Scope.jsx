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
        this.state = {}
        console.log(this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(requestScopesIfNeeded(this.props.versionId));
        this.state = {
            value: this.props.value,
        }
    }

    handleChange(e){
        this.setState({
            value: e.target.value
        });

    }


    render() {
        var inputLabel;
        if (this.props.label) {
            inputLabel = <label className='control-label'><span>{this.props.label}</span></label>
        }

        var data = [];
        this.props.refTables.scopesData.scopes.map(scope => {
                if (scope.versionId === this.props.versionId) {
                    scope.scopes.data.map(value => {
                        data.push(value);
                    })
                }
            }
        );

        return (
            <div>
                {inputLabel}
                <Input type='select' options={data} value={this.state.value} onChange={this.handleChange}>
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