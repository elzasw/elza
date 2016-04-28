import React from 'react';
import ReactDOM from 'react-dom';
import {DropdownButton} from 'react-bootstrap';
import {AbstractReactComponent} from 'components/index.jsx';

var ControllableDropdownButton = class ControllableDropdownButton extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getOpenValue', 'handleToggle', 'setOpen', 'focusFirstMenuItem')

        this.state = {
            open: this.getOpenValue(props, {})
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            open: this.getOpenValue(nextProps, this.state)
        })
    }

    componentDidMount(){
    }

    getOpenValue(props, state) {
        var open
        if (typeof props.open !== 'undefined') {
            open = props.open
        } else if (typeof state.open !== 'undefined') {
            open = state.open
        } else {
            open = false
        }

        return open
    }

    focusFirstMenuItem() {
        // TODO - není dobré řešení, ale v tuto chvíli mě jiné nenapadá
        var child = this.props.children[0]
        var ref
        if (typeof child.ref !== 'undefined' && child.ref !== null) {
            ref = child.ref
        } else {
            ref = 'firstMenuItem'
        }
        var el = ReactDOM.findDOMNode(this.refs[ref])
        el.children[0].focus()
    }

    handleToggle(isOpen) {
        this.setState({
            open: isOpen
        }, ()=>{isOpen && this.focusFirstMenuItem()})
        this.props.onToggle && this.props.onToggle(isOpen)
    }

    setOpen(open) {
        this.handleToggle(open)
    }

    render() {
        const {open, onToggle, ...other} = this.props

        var children = React.Children.map(this.props.children, (child, index) => {
            var opts = {}
            if (index === 0) {
                if (typeof child.ref !== 'undefined' && child.ref !== null) {
                    opts.ref = child.ref
                } else {
                    opts.ref = 'firstMenuItem'
                }
            }
            return React.cloneElement(child, opts);
        })

        return (
            <DropdownButton
                open={this.state.open}
                {...other}
                onToggle={this.handleToggle}
            >
                {children}
            </DropdownButton>
        );
          
    }
}


module.exports = ControllableDropdownButton
