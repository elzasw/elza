/**
 *  Komponenta pro vyhledávání
 *
 *  Pro inicializaci staci naimportovat: import {Search} from 'components/index.jsx';
 *
 **/

import React from 'react';

import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import ReactDOM from 'react-dom'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
require('./Scope.less');
import {indexById} from 'stores/app/utils.jsx';


/**
 *  Komponenta pro scope
 *  @param versionId zadat null nebo id verze
 *  <Scope versionId={null} label='Scope'/>
 **/
const Scope = class Scope extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(requestScopesIfNeeded(nextProps.versionId));
    }

    render() {
        let data = [];
        const {store: {scopes}, ...other} = this.props;
        const index = indexById(scopes, this.props.versionId, 'versionId');
        if (index !== null && scopes[index].scopes) {
            data = scopes[index].scopes;
        }

        return (
            <FormInput componentClass='select' options={data} {...other}>
                <option />
                {data.map((i)=> {return <option value={i.id} key={i.id}>{i.name}</option>})}
            </FormInput>
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