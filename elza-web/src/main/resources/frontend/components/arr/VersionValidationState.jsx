/**
 * Komponenta pro zobrazení stavu verze
 */

require('./VersionValidationState.less');

import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components';

var VersionValidationState = class VersionValidationState extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        var msg = null;

        if (this.props.isFetching) {
            msg = <span><Icon glyph="fa-refresh"/>{i18n('arr.fa.versionValidation.running')}</span>
        } else if (this.props.errExist) {
            msg = <span><Icon glyph="fa-exclamation-triangle"/>{i18n('arr.fa.versionValidation.count', this.props.count)}</span>
        } else {
            msg = <span><Icon glyph="fa-check"/>{i18n('arr.fa.versionValidation.ok')}</span>
        }

        return (
            <div className="version-state">{msg}</div>
        )
    }
};


VersionValidationState.propTypes = {
    count: React.PropTypes.number.isRequired,
    errExist: React.PropTypes.bool.isRequired,
    isFetching: React.PropTypes.bool.isRequired
};

module.exports = VersionValidationState;



