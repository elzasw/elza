import PropTypes from 'prop-types';
import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
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
                <Button className="reset-button" title={i18n('arr.panel.reset')} onClick={this.handleReset}>
                    <Icon glyph="fa-times" />
                </Button>
                <span className="title">{i18n('arr.panel.title', this.props.name)}</span>
            </div>
        );
    }
}

export default ArrPanel;
