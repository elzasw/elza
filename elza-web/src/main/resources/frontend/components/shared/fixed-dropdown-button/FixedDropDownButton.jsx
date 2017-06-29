import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import omit from 'lodash-compat/object/omit';
import pick from 'lodash-compat/object/pick';
import {Dropdown, Button, DropdownMenu} from 'react-bootstrap';
import './FixedDropDownButton.less';
import AbstractReactComponent from "../../AbstractReactComponent";

class FixedDropDownButton extends AbstractReactComponent {
    state = {marginSide: 0, marginTop: 0};

    componentDidMount() {
        this.setSizes();
    }
    componentDidUpdate() {
        this.setSizes();
    }

    setSizes = () => {
        const dropMenu = ReactDOM.findDOMNode(this.refs.dropdown).childNodes;

        dropMenu[1].style.display = "block";
        const calHeight = dropMenu[1].offsetHeight;
        dropMenu[1].style.display = "";

        const newState = {
            marginSide: this.props.pullRight ?
            document.body.getBoundingClientRect().right - dropMenu[0].getBoundingClientRect().right :
            dropMenu[0].getBoundingClientRect().left - document.body.getBoundingClientRect().left,
            marginTop: this.props.dropup ?  -calHeight : dropMenu[0].getBoundingClientRect().height
        };

        (this.state.marginSide !== newState.marginSide || this.state.marginTop !== newState.marginTop) && this.setState(newState);
    }

    render() {
        let { bsStyle, bsSize, disabled, pullRight} = this.props;
        let { title, children, ...props } = this.props;
        let { marginTop, marginSide } = this.state;

        let dropdownProps = pick(props, Object.keys(Dropdown.ControlledComponent.propTypes));
        let toggleProps = omit(props, Object.keys(Dropdown.ControlledComponent.propTypes));

        const style = {
            'marginTop': marginTop + 'px'
        };
        style[pullRight ? 'right' : 'left'] = marginSide + 'px';

        return (
            <span className='fixed-dropdown'>
                <Dropdown {...dropdownProps}
                    bsSize={bsSize}
                    bsStyle={bsStyle}
                    ref='dropdown'
                >
                    <Dropdown.Toggle
                        {...toggleProps}
                        disabled={disabled}
                    >
                        {title}
                    </Dropdown.Toggle>
                    <Dropdown.Menu style={style}>
                        {children}
                    </Dropdown.Menu>
                </Dropdown>
        </span>
        );
    }
};

FixedDropDownButton.propTypes = {
    disabled: React.PropTypes.bool,
    bsStyle: Button.propTypes.bsStyle,
    bsSize: Button.propTypes.bsSize,

    /**
     * When used with the `title` prop, the noCaret option will not render a caret icon, in the toggle element.
     */
    noCaret: React.PropTypes.bool,
    title: React.PropTypes.node.isRequired,

    ...Dropdown.propTypes,
};

FixedDropDownButton.defaultProps = {
    disabled: false,
    pullRight: false,
    dropup: false,
    navItem: false,
    noCaret: false
};

export default FixedDropDownButton;
