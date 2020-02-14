import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom'
import {connect} from 'react-redux'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {indexById} from 'stores/app/utils.jsx';
import AbstractReactComponent from "../../AbstractReactComponent";
import FormInput from "../form/FormInput";

import './Scope.scss';

/**
 *  Komponenta pro scope
 *
 *  @param versionId zadat null nebo id verze nebo -1 pro všechny scopes
 *  <Scope label='Scope'/>
 */
class Scope extends AbstractReactComponent {

    static propTypes = {
        versionId: PropTypes.oneOfType([PropTypes.number, PropTypes.object]),
        value: PropTypes.oneOfType([PropTypes.number, PropTypes.string])
    };

    static defaultProps = {
        versionId: -1
    };

    componentDidMount() {
        const {store: {scopes}, versionId} = this.props;
        this.props.dispatch(requestScopesIfNeeded(versionId));

        const index = indexById(scopes, versionId, 'versionId');
        if (index !== null) {
            const data = scopes[index].scopes;
            if (data && data.length === 1) {
                this.props.onChange(data[0].id);
            }
        }
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(requestScopesIfNeeded(nextProps.versionId));

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

        return <FormInput as='select' options={data} {...other}>
            <option key="null" />
            {data.map(i => <option value={i.id} key={i.id}>{i.name}</option>)}
        </FormInput>
    }
}

export default connect((state) => ({store: state.refTables.scopesData}))(Scope);
