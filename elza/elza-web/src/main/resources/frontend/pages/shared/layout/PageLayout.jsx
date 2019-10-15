import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux'
import classNames from 'classnames';
import {Splitter, ToggleContent} from 'components/shared';
import {splitterResize} from 'actions/global/splitter.jsx';

import './PageLayout.less';

/**
 * Standardní layout stránky, který ribbon, obsahuje levý panel, prostřední panel a pravý panel, které jsou odděleny splitterem.
 */
class PageLayout extends React.Component {

    state = {
        ribbonOpened: true
    };

    handleRibbonShowHide = (opened) => {
        this.setState({ribbonOpened: opened});
    };

    render() {
        const {className, status, ribbon, splitter, leftPanel, centerPanel, rightPanel} = this.props;
        const {ribbonOpened} = this.state;
        const cls = classNames(className, {
            'app-container': true,
            'app-exists-status': status != null,
            noRibbon: !ribbonOpened,
        });

        return (
            <div className={cls}>
                <div className='app-header'>
                    <ToggleContent className="ribbon-toggle-container" opened={ribbonOpened} onShowHide={this.handleRibbonShowHide}>
                        {ribbon}
                    </ToggleContent>
                </div>
                <div className='status-header'>
                    {status}
                </div>
                <div className='app-content'>

                    <Splitter
                        leftSize={splitter.leftWidth}
                        rightSize={splitter.rightWidth}
                        onChange={({leftSize, rightSize}) => {this.props.dispatch(splitterResize(leftSize, rightSize))}}
                        left={leftPanel}
                        center={centerPanel}
                        right={rightPanel}
                    />
                </div>
            </div>
        )
    }
}

export default connect()(PageLayout);


