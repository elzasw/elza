import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components/index.jsx';
import {Tooltip, Overlay} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'

/**
 * Obalující komponenta pro tooltip umožňující hover v tooltipu
 *
 * @author Jakub Randák
 * @since 22.11.2016
 */
class TooltipTrigger extends AbstractReactComponent {

    static PropTypes = {
        content: React.PropTypes.object.isRequired,
        placement: React.PropTypes.oneOf(['left', 'right', 'top', 'bottom']),
        holdOnHover: React.PropTypes.bool,
        holdOnFocus: React.PropTypes.bool,
        delay: React.PropTypes.number,
        showDelay: React.PropTypes.number,
        hideDelay: React.PropTypes.number,
        focusDelay: React.PropTypes.number,
        focusShowDelay: React.PropTypes.number,
        focusHideDelay: React.PropTypes.number,
    };

    constructor(props) {
        super(props);
        this.state = {
            showTooltip: false,
            overTooltip: false,
            focus: false
        };
    };
    componentWillMount(){
      const {delay = 100, showDelay, hideDelay, focusDelay, focusShowDelay, focusHideDelay} = this.props;
      if(showDelay === null || typeof showDelay === "undefined"){
          this.showDelay = delay;
      }
      if(hideDelay === null || typeof hideDelay === "undefined"){
          this.hideDelay = delay;
      }
      if((focusDelay === null || typeof focusDelay === "undefined") && (focusShowDelay === null || typeof focusShowDelay === "undefined")){
          this.focusShowDelay = delay;
      } else if((focusDelay !== null || typeof focusDelay !== "undefined") && (focusShowDelay === null || typeof focusShowDelay === "undefined")){
          this.focusShowDelay = focusDelay;
      }
      if((focusDelay === null || typeof focusDelay === "undefined") && (focusHideDelay === null || typeof focusHideDelay === "undefined")){
          this.focusHideDelay = delay;
      } else if((focusDelay !== null || typeof focusDelay !== "undefined") && (focusHideDelay === null || typeof focusHideDelay === "undefined")){
          this.focusHideDelay = focusDelay;
      }
    }
    componentWillUnmount(){
      clearTimeout(this.hoverDelay);
    }

    /**
    * Zobrazení a skrývání tooltipu
    * @param {Bool} show - true - zobrazí tooltip, false - skryje tooltip
    * @param {Number} delay - udává zpožděn zobrazení tooltipu
    */
    showTooltip(show, delay) {
    if(this.hoverDelay !== null){clearTimeout(this.hoverDelay)}
    this.hoverDelay = setTimeout(() => {
            if (!this.state.focus && !this.state.overTooltip && !show) {
                this.setState({showTooltip: false})
            } else {
                this.setState({showTooltip: true})
            }
        }, delay);
    }

    /**
    * Přepínání state pro hover
    * @param {Bool} hover - true - přepne this.state.overTooltip na true, false - přepne this.state.overTooltip na false
    */
    handleTooltipHover(hover) {
        if (this.props.holdOnHover) {
            var delay = hover ? this.showDelay : this.hideDelay;
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
    handleFocus(focus) {
      if (this.props.holdOnFocus) {
        var delay = focus ? this.focusShowDelay : this.focusHideDelay;
        this.setState({
            focus: focus
        }, () => {
            this.showTooltip(focus, delay)
        })
      }
    }

    render() {
        const {content, children, placement} = this.props;
        return (
            <div ref="ttTarget" onFocus={() => this.handleFocus(true)} onBlur={() => this.handleFocus(false)} onMouseOver={() => this.showTooltip(true, this.showDelay)} onMouseLeave={() => this.showTooltip(false, this.hideDelay)}>
                {children}
                <Overlay show={this.state.showTooltip} placement={placement} target={() => this.refs.ttTarget}>
                    <Tooltip onMouseOver={() => this.handleTooltipHover(true)} onMouseLeave={() => this.handleTooltipHover(false)} id='tt'>
                        {content}
                    </Tooltip>
                </Overlay>
            </div>
        )
    }
}

module.exports = TooltipTrigger;
