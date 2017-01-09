import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components/index.jsx';
import {Button, Tooltip, Overlay} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'

require("./TooltipTrigger.less")

const TOOLTIP_WINDOW_PADDING = 40;

/**
 * Obalující komponenta pro tooltip umožňující hover v tooltipu
 *
 * @author Jakub Randák
 * @since 22.11.2016
 */
class TooltipTrigger extends AbstractReactComponent {

    static PropTypes = {
        content: React.PropTypes.object.isRequired,
        placement: React.PropTypes.oneOf(['left', 'right', 'top', 'bottom', 'vertical', 'horizontal', 'auto']),
        holdOnHover: React.PropTypes.bool,
        holdOnFocus: React.PropTypes.bool,
        delay: React.PropTypes.number,
        showDelay: React.PropTypes.number,
        hideDelay: React.PropTypes.number,
        focusDelay: React.PropTypes.number,
        focusShowDelay: React.PropTypes.number,
        focusHideDelay: React.PropTypes.number,
    };

    static defaultProps = {
        showDelay: 300,
        hideDelay: 500,
        focusShowDelay: 250,
        focusHideDelay: 1,
        placement: "auto"
    }

    constructor(props) {
        super(props);

        this.state = {
            showTooltip: false,
            overTooltip: false,
            focus: false,
            placement: null,    // určí se až před zobrazením
        };
    };

    componentWillMount() {
    }

    componentWillUnmount() {
        clearTimeout(this.hoverDelay);
    }

    /**
     * Zobrazení a skrývání tooltipu
     * @param {Bool} show - true - zobrazí tooltip, false - skryje tooltip
     * @param {Number} delay - udává zpožděn zobrazení tooltipu
     */
    showTooltip = (show, delay) => {
        if (this.hoverDelay !== null) {
            clearTimeout(this.hoverDelay);
        }

        // Zobrazení tooltipu se spožděním
        this.hoverDelay = setTimeout(() => {
            if (!this.state.focus && !this.state.overTooltip && !show) {
                this.setState({showTooltip: false});
            } else {
                const {placement} = this.props;

                // Určení pozice tooltipu
                const ww = window.innerWidth;
                const wh = window.innerHeight;
                let usePlacement;
                let maxWidth, maxHeight;
                const node = ReactDOM.findDOMNode(this.refs.ttTarget);
                const rect = node.getBoundingClientRect();
                switch (placement) {
                    case "left":
                        maxHeight = wh - (2 * TOOLTIP_WINDOW_PADDING);
                        maxWidth = rect.left - TOOLTIP_WINDOW_PADDING;
                        usePlacement = placement;
                        break;
                    case "right":
                        maxHeight = wh - (2 * TOOLTIP_WINDOW_PADDING);
                        maxWidth = (ww - rect.right) - TOOLTIP_WINDOW_PADDING;
                        usePlacement = placement;
                        break;
                    case "top":
                        maxWidth = ww - (2 * TOOLTIP_WINDOW_PADDING);
                        maxHeight = rect.top - TOOLTIP_WINDOW_PADDING;
                        usePlacement = placement;
                        break;
                    case "bottom":
                        maxWidth = ww - (2 * TOOLTIP_WINDOW_PADDING);
                        maxHeight = (wh - rect.bottom) - TOOLTIP_WINDOW_PADDING;
                        usePlacement = placement;
                        break;
                    case "horizontal": {
                        maxHeight = wh - (2 * TOOLTIP_WINDOW_PADDING);
                        if (rect.left > ww - rect.right) {
                            usePlacement = "left";
                            maxWidth = rect.left - TOOLTIP_WINDOW_PADDING;
                        } else {
                            usePlacement = "right";
                            maxWidth = (ww - rect.right) - TOOLTIP_WINDOW_PADDING;
                        }
                    }
                    break;
                    case "vertical": {
                        maxWidth = ww - (2 * TOOLTIP_WINDOW_PADDING);
                        if (rect.top > wh - rect.bottom) {
                            usePlacement = "top";
                            maxHeight = rect.top - TOOLTIP_WINDOW_PADDING;
                        } else {
                            usePlacement = "bottom";
                            maxHeight = (wh - rect.bottom) - TOOLTIP_WINDOW_PADDING;
                        }
                    }
                    break;
                    case "auto": {
                        const left = rect.left * wh;
                        const right = (ww - rect.right) * wh;
                        const top = rect.top * ww;
                        const bottom = (wh - rect.bottom) * ww;

                        const nums = [top, right, bottom, left];
                        const dirs = ['top', 'right', 'bottom', 'left'];
                        var max = 0;
                        for (let a=1; a<nums.length; a++) {
                            if (nums[a] > nums[max]) {
                                max = a;
                            }
                        }
                        usePlacement = dirs[max];

                        switch (usePlacement) {
                            case "left":
                                maxHeight = wh - (2 * TOOLTIP_WINDOW_PADDING);
                                maxWidth = rect.left - TOOLTIP_WINDOW_PADDING;
                                break;
                            case "right":
                                maxHeight = wh - (2 * TOOLTIP_WINDOW_PADDING);
                                maxWidth = (ww - rect.right) - TOOLTIP_WINDOW_PADDING;
                                break;
                            case "top":
                                maxWidth = ww - (2 * TOOLTIP_WINDOW_PADDING);
                                maxHeight = rect.top - TOOLTIP_WINDOW_PADDING;
                                break;
                            case "bottom":
                                maxWidth = ww - (2 * TOOLTIP_WINDOW_PADDING);
                                maxHeight = (wh - rect.bottom) - TOOLTIP_WINDOW_PADDING;
                                break;
                        }
                    }

                    break;
                }

                // Nastavení zobrazení včetně spočteného placement
                this.setState({
                    showTooltip: true,
                    placement: usePlacement,
                    maxWidth,
                    maxHeight
                });
            }
        }, delay);
    };

    getDelay = (show) => {
        const {delay, showDelay, hideDelay} = this.props;
        if (show) {
            if (showDelay === null || typeof showDelay === "undefined"){
                return delay;
            } else {
                return showDelay;
            }
        } else {
            if (hideDelay === null || typeof hideDelay === "undefined") {
                return delay;
            } else {
                return hideDelay;
            }
        }
    };

    getFocusDelay = (show) => {
        const {focusDelay, focusShowDelay, focusHideDelay} = this.props;
        if (show) {
            if (focusShowDelay === null || typeof focusShowDelay === "undefined"){
                return focusDelay;
            } else {
                return focusShowDelay;
            }
        } else {
            if (focusHideDelay === null || typeof focusHideDelay === "undefined") {
                return focusDelay;
            } else {
                return focusHideDelay;
            }
        }
    };

    /**
     * Přepínání state pro hover
     * @param {Bool} hover - true - přepne this.state.overTooltip na true, false - přepne this.state.overTooltip na false
     */
    handleTooltipHover = (hover) => {
        console.log("###handleTooltipHover", hover)

        if (this.props.holdOnHover) {
            const delay = this.getDelay(hover);
            this.setState({
                overTooltip: hover
            }, () => {
                this.showTooltip(hover, delay);
            })
        }
    }

    /**
     * Přepínání state pro focus
     * @param {Bool} focus - true - přepne this.state.focus na true, false - přepne this.state.focus na false
     */
    handleFocus = (focus) => {
        console.log("###handleFocus", focus)

        if (this.props.holdOnFocus) {
            const delay = this.getFocusDelay(focus);
            this.setState({
                focus: focus
            }, () => {
                this.showTooltip(focus, delay)
            })
        }
    }

    render() {
        const {className, content, children} = this.props;
        const {placement, maxWidth, maxHeight} = this.state;

        return (
            <span
                className={className}
                ref="ttTarget"
                onFocus={() => this.handleFocus(true)}
                onBlur={() => this.handleFocus(false)}
                onMouseOver={() => this.showTooltip(true, this.getDelay(true))}
                onMouseLeave={() => this.showTooltip(false, this.getDelay(false))}
            >
                {children}
                <Overlay
                    show={this.state.showTooltip}
                    placement={placement}
                    target={() => this.refs.ttTarget}
                >
                    <Tooltip
                        onMouseOver={() => this.handleTooltipHover(true)}
                        onMouseLeave={() => this.handleTooltipHover(false)}
                        id='tt'
                    >
                        <div className="tooltip-inner-content" style={{maxWidth: maxWidth + "px", maxHeight: maxHeight + "px"}}>
                            {content}
                        </div>
                    </Tooltip>
                </Overlay>
            </span>
        )
    }
}

module.exports = TooltipTrigger;