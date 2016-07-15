/**
 * Komponenta pro vytvoření stejných poliček pro editaci záznamu
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, NoFocusButton, AbstractReactComponent, DropDownTree, FormInput} from 'components/index.jsx';
import {connect} from 'react-redux'

const RegistryLabel = class RegistryLabel extends AbstractReactComponent {
    constructor(props){
        super(props);
        this.bindMethods(
            'handleChange',
            'handleKeyUp',
            'focus'
        );

        this.state = {
            variant: this.props.value
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            variant: nextProps.value
        })
    }

    handleKeyUp(e) {
        e.keyCode == 13 && this.props.onEnter && this.props.onEnter(e);
    }

    handleChange(e) {
        this.setState({
            variant: e.target.value                                  // uložení zadaného řezezce ve stavu komponenty
        });
    }

    focus() {
        this.refs.input.getInputDOMNode().focus()
    }

    render() {
        const {label, disabled, onBlur, onClickDelete} = this.props;
        return (
            <div className="registry-label">
                <div className='title' title={label}>{label}</div>
                <div className="desc-item-value-container">
                    <span>
                        <FormInput
                            disabled={disabled}
                            ref='input'
                            type='text'
                            value={this.state.variant}
                            onChange={this.handleChange}
                            onKeyUp={this.handleKeyUp}
                            onBlur={onBlur}
                        />
                    </span>
                    <NoFocusButton disabled={disabled} onClick={onClickDelete}><Icon glyph='fa-times' /></NoFocusButton>
                </div>
            </div>
        )
    }
};

RegistryLabel.propTypes = {
    disabled: React.PropTypes.bool.isRequired,
    onEnter: React.PropTypes.func.isRequired,
    onBlur: React.PropTypes.func.isRequired,
    value: React.PropTypes.string.isRequired
};

module.exports = connect(null, null, null, { withRef: true })(RegistryLabel);