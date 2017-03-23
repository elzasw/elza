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
 *  @param versionId zadat null nebo id verze nebo -1 pro všechny scopes
 *  <Scope label='Scope'/>
 */
class Scope extends AbstractReactComponent {

    static PropTypes = {
        versionId: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.object]),
        value: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string])
    };

    static defaultProps = {
        versionId: -1
    };

    componentDidMount() {
        const {store: {scopes}, versionId} = this.props;
        this.dispatch(requestScopesIfNeeded(versionId));

        const index = indexById(scopes, versionId, 'versionId');
        if (index !== null) {
            const data = scopes[index].scopes;
            if (data && data.length === 1) {
                this.props.onChange(data[0].id);
            }
        }
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(requestScopesIfNeeded(nextProps.versionId));

        const {store: {scopes}, versionId} = nextProps;
        const index = indexById(scopes, versionId, 'versionId');
        const oldIndex = indexById(this.props.store.scopes, versionId, 'versionId');
        if (index !== null && index !== oldIndex) {
            const data = scopes[index].scopes;
            if (data && data.length === 1) {
                nextProps.onChange(data[0].id);
            }
        }
    }

    render() {
        let data = [];
        const {store: {scopes}, versionId, ...other} = this.props;
        const index = indexById(scopes, versionId, 'versionId');
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
