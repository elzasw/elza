/**
 *  Komponenta pro vyhledávání
 *
 *  Pro inicializaci staci naimportovat: import {Search} from 'components/index.jsx';
 *
 **/

import React from 'react';

import {Button, Input} from 'react-bootstrap';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import ReactDOM from 'react-dom'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
require ('./Scope.less');
import {indexById} from 'stores/app/utils.jsx';


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
        this.dispatch(requestScopesIfNeeded(nextProps.versionId));
    }

    render() {
        var data = [];
        let index = indexById(this.props.store.scopes, this.props.versionId, 'versionId');
        if (index !== null && this.props.store.scopes[index].scopes) {
            data = this.props.store.scopes[index].scopes;
        }
        var {refTables, ...other} = this.props;

        return (
            <Input type='select' options={data} {...other}>
                <option />
                {data.map((i)=> {return <option value={i.id} key={i.id}>{i.name}</option>})}
            </Input>
        );
    }
};

Scope.propTypes = {
    value: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string])
};

function mapStateToProps(state) {
    return {
        store: state.refTables.scopesData
    }
}

module.exports = connect(mapStateToProps)(Scope);