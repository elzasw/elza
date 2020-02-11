import './VersionValidationState.less';;

/**
 * Komponenta pro zobrazení stavu verze
 */

import PropTypes from 'prop-types';

import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';

var VersionValidationState = class VersionValidationState extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        var msg = null;

        if (this.props.isFetching) {
            msg = <span><Icon glyph="fa-refresh"/>{i18n('arr.fund.versionValidation.running')}</span>
        } else if (this.props.errExist) {
            msg = <span className="err"><Icon glyph="fa-exclamation-triangle"/>{i18n('arr.fund.versionValidation.count', this.props.count)}</span>
        } else {
            msg = <span className="ok"><Icon glyph="fa-check"/>{i18n('arr.fund.versionValidation.ok')}</span>
        }

        return (
            <div className="version-state">{msg}</div>
        )
    }
};


VersionValidationState.propTypes = {
    count: PropTypes.number.isRequired,
    errExist: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired
};

export default VersionValidationState;



