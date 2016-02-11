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
import {requestScopesIfNeeded} from 'actions/refTables/scopesData'
require ('./Scope.less');
import {indexById} from 'stores/app/utils';


/**
 *  Komponenta pro scope
 *  @param versionId zadat null nebo id verze
 *  <Scope versionId={null} label='Scope'/>
 **/
var Scope = class Scope extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    componentWillReceiveProps(nextProps) {
        /*let index = indexById(this.props.refTables.scopesData.scopes, this.props.versionId, 'versionId');
         if (index !== null) {
         this.props.refTables.scopesData.scopes[index].isFetching
         }*/
        this.dispatch(requestScopesIfNeeded(nextProps.versionId));
    }

    componentDidMount(){
    }

    render() {
        /*
        this.props.refTables.scopesData.scopes.map(scope => {
                if (scope.versionId === this.props.versionId) {
                    data = scope.scopes.data;
                }
            }
         );*/
        var data = [];
        let index = indexById(this.props.refTables.scopesData.scopes, this.props.versionId, 'versionId');
        if (index !== null && this.props.refTables.scopesData.scopes[index].data) {
            data = this.props.refTables.scopesData.scopes[index].data;
        }

        var {refTables, ...other} = this.props;

        return (
            <Input type='select' options={data} {...other}>
                <option ></option>
                {data.map((i)=> {return <option value={i.id} key={i.id}>{i.name}</option>})}
            </Input>
        );
    }
}

Scope.propTypes = {
    value: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string]),

}

function mapStateToProps(state) {
    const {refTables} = state
    return {
        refTables
    }
}

module.exports = connect(mapStateToProps)(Scope);