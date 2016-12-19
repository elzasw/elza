import React from 'react';
import ReactDOM from 'react-dom'
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {indexById} from 'stores/app/utils.jsx';

import './Scope.less';


/**
 *  Komponenta pro scope
 *
 *  @param versionId zadat null nebo id verze
 *  <Scope versionId={null} label='Scope'/>
 */
class Scope extends AbstractReactComponent {

    static PropTypes = {
        value: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string])
    };

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

        return <FormInput componentClass='select' options={data} {...other}>
            <option key="null" />
            {data.map(i => <option value={i.id} key={i.id}>{i.name}</option>)}
        </FormInput>
    }
}

export default connect((state) => ({store: state.refTables.scopesData}))(Scope);