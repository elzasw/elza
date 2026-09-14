import './VersionValidationState.scss';
import PropTypes from 'prop-types';

import React from 'react';
import {AbstractReactComponent, Icon} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { arrPanelMessages } from './panelMessages';

/**
 * Komponenta pro zobrazení stavu verze
 */

class VersionValidationState extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        var msg = null;

        if (this.props.isFetching) {
            msg = (
                <span>
                    <Icon glyph="fa-refresh" />
                    {<FormattedMessage {...arrPanelMessages.fundVersionValidationRunning} />}
                </span>
            );
        } else if (this.props.errExist) {
            msg = (
                <span className="err">
                    <Icon glyph="fa-exclamation-triangle" />
                    {this.props.intl.formatMessage(arrPanelMessages.fundVersionValidationCount, { 0: this.props.count })}
                </span>
            );
        } else {
            msg = (
                <span className="ok">
                    <Icon glyph="fa-check" />
                    {<FormattedMessage {...arrPanelMessages.fundVersionValidationOk} />}
                </span>
            );
        }

        return <div className="version-state">{msg}</div>;
    }
}

VersionValidationState.propTypes = {
    count: PropTypes.number.isRequired,
    errExist: PropTypes.bool.isRequired,
    isFetching: PropTypes.bool.isRequired,
};

export default injectIntl(VersionValidationState);
