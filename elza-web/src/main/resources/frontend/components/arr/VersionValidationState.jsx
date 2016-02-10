/**
 * Komponenta pro zobrazení stavu verze
 */

import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components';

var VersionValidationState = class VersionValidationState extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        if (this.props.isFetching) {
            return <span><Icon glyph="fa-refresh"/> {i18n('arr.fa.versionValidation.running')}</span>;
        } else if (this.props.errExist) {
            return <span><Icon
                glyph="fa-exclamation-triangle"/> {i18n('arr.fa.versionValidation.count', this.props.count)}</span>;
        } else {
            return <span><Icon glyph="fa-check"/> {i18n('arr.fa.versionValidation.ok')}</span>;
        }
    }
};


VersionValidationState.propTypes = {
    count: React.PropTypes.number.isRequired,
    errExist: React.PropTypes.bool.isRequired,
    isFetching: React.PropTypes.bool.isRequired
};

module.exports = VersionValidationState;



