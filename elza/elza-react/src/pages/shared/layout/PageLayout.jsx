import React from 'react';

import {connect} from 'react-redux';
import classNames from 'classnames';
import {Splitter, ToggleContent} from 'components/shared';
import {splitterResize} from 'actions/global/splitter.jsx';
import {DEFAULT_SPLITTER_SIZES, SPLITTER_AREA_GLOBAL} from 'stores/app/global/splitter.jsx';

import './PageLayout.scss';

/**
 * Standardní layout stránky, který ribbon, obsahuje levý panel, prostřední panel a pravý panel, které jsou odděleny splitterem.
 *
 * Rozměry panelů si layout bere ze store podle oblasti (`area`) - stránky bez vlastní oblasti
 * používají oblast globální.
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
        const { className, status, ribbon, splitterSizes, sidebar, leftPanel, centerPanel, rightPanel, area } = this.props;
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
                    {sidebar && <div className="app-sidebar">{sidebar}</div>}
                    <div className="app-content-main">
                        <Splitter
                            leftSize={splitterSizes.leftWidth}
                            rightSize={splitterSizes.rightWidth}
                            onChange={({leftSize, rightSize}) => {
                                this._pendingLeftSize = leftSize;
                                this._pendingRightSize = rightSize;
                            }}
                            onDragFinished={() => {
                                if (this._pendingLeftSize !== null || this._pendingRightSize !== null) {
                                    this.props.dispatch(splitterResize(this._pendingLeftSize ?? splitterSizes.leftWidth, this._pendingRightSize ?? splitterSizes.rightWidth, area));
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
            </div>
        );
    }
}

const mapStateToProps = (state, ownProps) => {
    const splitters = state.splitter.splitters;
    const area = ownProps.area || SPLITTER_AREA_GLOBAL;
    return {
        splitterSizes: splitters[area] || splitters[SPLITTER_AREA_GLOBAL] || DEFAULT_SPLITTER_SIZES,
    };
};

export default connect(mapStateToProps)(PageLayout);
