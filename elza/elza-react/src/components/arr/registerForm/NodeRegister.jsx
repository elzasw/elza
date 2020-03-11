import PropTypes from 'prop-types';
import React from 'react';
import {AbstractReactComponent} from 'components/shared';
import {decorateAutocompleteValue} from './../nodeForm/DescItemUtils.jsx';

import './NodeRegister.scss';
import RegistryField from '../../registry/RegistryField';

class NodeRegister extends AbstractReactComponent {

    static propTypes = {
        onChange: PropTypes.func.isRequired,
        onCreateRecord: PropTypes.func.isRequired,
        onDetail: PropTypes.func.isRequired,
        versionId: PropTypes.number,
        item: PropTypes.object,
        closed: PropTypes.bool,
        value: PropTypes.number
    };

    render() {
        const {item, closed, ...otherProps} = this.props;
        const record = item.record ? item.record : null;


        return <div className='link-value'>
            <RegistryField
                ref="registryField"
                {...otherProps}
                value={record}
                footer={true}
                detail={true}
                {...decorateAutocompleteValue(this, item.hasFocus, item.error.value, closed, ['autocomplete-record'])}
            />
        </div>
    }
}

export default NodeRegister
