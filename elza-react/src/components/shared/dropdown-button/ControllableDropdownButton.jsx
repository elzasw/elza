import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {DropdownButton} from 'react-bootstrap';
import AbstractReactComponent from "../../AbstractReactComponent";

class ControllableDropdownButton extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            open: this.getOpenValue(props, {})
        }
    }

    static propsTypes = {
        id: PropTypes.string.isRequired,
        children: PropTypes.array.isRequired,
        onToggle: PropTypes.func,
        open: PropTypes.bool
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState({
            open: this.getOpenValue(nextProps, this.state)
        })
    }

    getOpenValue = (props, state) => {
        let open
        if (typeof props.open !== 'undefined') {
            open = props.open
        } else if (typeof state.open !== 'undefined') {
            open = state.open
        } else {
            open = false
        }

        return open
    };

    focusFirstMenuItem = () => {
        // TODO - není dobré řešení, ale v tuto chvíli mě jiné nenapadá
        const child = this.props.children[0];
        let ref;
        if (typeof child.ref !== 'undefined' && child.ref !== null) {
            ref = child.ref
        } else {
            ref = 'firstDropdown.Item'
        }
        const el = ReactDOM.findDOMNode(this.refs[ref]);
        el.children[0].focus()
    };

    handleToggle = (isOpen) => {
        this.setState({
            open: isOpen
        }, ()=>{isOpen && this.focusFirstDropdown.Item()});
        this.props.onToggle && this.props.onToggle(isOpen)
    };

    setOpen = (open) => {
        this.handleToggle(open)
    };

    render() {
        const {open, onToggle, id, ...other} = this.props;

        const children = React.Children.map(this.props.children, (child, index) => {
            const opts = {};
            if (index === 0) {
                if (typeof child.ref !== 'undefined' && child.ref !== null) {
                    opts.ref = child.ref
                } else {
                    opts.ref = 'firstDropdown.Item'
                }
            }
            return React.cloneElement(child, opts);
        });

        return (
            <DropdownButton
                id={id}
                open={this.state.open}
                {...other}
                onToggle={this.handleToggle}
            >
                {children}
            </DropdownButton>
        );

    }
}


export default ControllableDropdownButton;
