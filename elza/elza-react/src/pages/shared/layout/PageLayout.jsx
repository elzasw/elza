import React from 'react';

import {connect} from 'react-redux';
import classNames from 'classnames';
import {Splitter, ToggleContent} from 'components/shared';
import {splitterResize} from 'actions/global/splitter.jsx';

import './PageLayout.scss';

/**
 * Standardní layout stránky, který ribbon, obsahuje levý panel, prostřední panel a pravý panel, které jsou odděleny splitterem.
 */
class PageLayout extends React.Component {
    state = {
        ribbonOpened: true,
    };

    _pendingLeftSize = null;
    _pendingRightSize = null;

    handleRibbonShowHide = opened => {
        this.setState({ribbonOpened: opened});
    };

    render() {
        const {ribbonOpened} = this.state;
        const { className, status, ribbon, splitter, leftPanel, centerPanel, rightPanel, area } = this.props;
        const cls = classNames(className, {
            'app-container': true,
            'app-exists-status': status != null,
            noRibbon: !ribbonOpened,
        });

        return (
            <div className={cls}>
                <div className="app-header">
                    <ToggleContent
                        className="ribbon-toggle-container"
                        opened={ribbonOpened}
                        onShowHide={this.handleRibbonShowHide}
                    >
                        {ribbon}
                    </ToggleContent>
                </div>
                <div className="status-header">{status}</div>
                <div className="app-content">
                    <Splitter
                        leftSize={splitter.leftWidth}
                        rightSize={splitter.rightWidth}
                        onChange={({leftSize, rightSize}) => {
                            this._pendingLeftSize = leftSize;
                            this._pendingRightSize = rightSize;
                        }}
                        onDragFinished={() => {
                            if (this._pendingLeftSize !== null || this._pendingRightSize !== null) {
                                this.props.dispatch(splitterResize(this._pendingLeftSize ?? splitter.leftWidth, this._pendingRightSize ?? splitter.rightWidth, area));
                                this._pendingLeftSize = null;
                                this._pendingRightSize = null;
                            }
                        }}
                        left={leftPanel}
                        center={centerPanel}
                        right={rightPanel}
                    />
                </div>
            </div>
        );
    }
}

export default connect()(PageLayout);
