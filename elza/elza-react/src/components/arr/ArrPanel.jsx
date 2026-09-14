import PropTypes from 'prop-types';
import React from 'react';
import {AbstractReactComponent, Icon} from 'components/shared';
import { injectIntl } from 'react-intl';
import { arrPanelMessages } from './panelMessages';
import {Button} from '../ui';

import './ArrPanel.scss';

class ArrPanel extends AbstractReactComponent {
    static propTypes = {
        name: PropTypes.string.isRequired,
        onReset: PropTypes.func.isRequired,
    };

    handleReset = () => {
        if (this.props.onReset) {
            this.props.onReset();
        } else {
            console.error('onReset not defined');
        }
    };

    render() {
        return (
            <div key="arr-panel" className="arr-panel">
                <Button className="reset-button" title={this.props.intl.formatMessage(arrPanelMessages.panelReset)} onClick={this.handleReset}>
                    <Icon glyph="fa-times" />
                </Button>
                <span className="title">{this.props.intl.formatMessage(arrPanelMessages.panelTitle, { 0: this.props.name })}</span>
            </div>
        );
    }
}

export default injectIntl(ArrPanel);
