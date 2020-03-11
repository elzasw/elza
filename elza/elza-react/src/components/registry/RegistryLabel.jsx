import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, FormInput, Icon, NoFocusButton} from 'components/shared';

/**
 * Komponenta pro vytvoření stejných poliček pro editaci záznamu
 */
class RegistryLabel extends AbstractReactComponent {

    state = {
        value: this.props.value,
    };

    static propTypes = {
        disabled: PropTypes.bool.isRequired,
        onEnter: PropTypes.func.isRequired,
        onBlur: PropTypes.func.isRequired,
        onDelete: PropTypes.func.isRequired,
        value: PropTypes.string.isRequired,
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState({
            value: nextProps.value,
        });
    }

    handleKeyUp = (e) => {
        e.keyCode === 13 && this.props.onEnter && this.props.onEnter(e);
    };

    handleChange = (e) => {
        this.setState({
            value: e.target.value,
        });
    };

    focus = () => {
        ReactDOM.findDOMNode(this.refs.input.refs.input).focus();
    };

    render() {
        const { label, disabled, onBlur, onDelete } = this.props;
        return <div className="registry-label">
            <div className='title' title={label}>{label}</div>
            <div className="desc-item-value-container">
                <span>
                    <FormInput
                        disabled={disabled}
                        ref='input'
                        type='text'
                        value={this.state.value}
                        onChange={this.handleChange}
                        onKeyUp={this.handleKeyUp}
                        onBlur={onBlur}
                    />
                </span>
                <NoFocusButton disabled={disabled} onClick={onDelete}><Icon glyph='fa-times'/></NoFocusButton>
            </div>
        </div>;
    }
}

export default RegistryLabel;
