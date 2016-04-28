
require ('./RegistryCoordinates.less');
import React from 'react';
import ReactDOM from 'react-dom';
import {Input, Button} from 'react-bootstrap';
import {objectFromWKT, wktFromTypeAndData, wktType} from 'components/Utils.jsx';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, NoFocusButton, Icon} from 'components/index.jsx';

var RegistryCoordinates = class RegistryCoordinates extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChangeValue', 'handleChangeDescription', 'handleKeyUp', 'focus');

        this.state = objectFromWKT(props.item.value);
    }

    componentWillReceiveProps(nextProps) {
        this.setState(objectFromWKT(nextProps.item.value));
    }

    focus() {
        this.refs.focusEl.focus()
    }

    handleKeyUp(e) {
        if (e.keyCode == 13) {
            if (this.props.item.value) {
                if (this.props.item.hasError === undefined) {
                    this.handleChangeValue({target: {value: this.state.data}});
                }
                this.props.onEnterKey && this.props.onEnterKey(e)
            } else {
                this.handleChangeValue({target: {value: ""}});
            }
        }
    }

    handleChangeDescription(e) {
        const val = e.target.value;
        if (val != this.props.item.description) {
            this.props.onChange({
                ...this.props.item,
                description: val
            });
        }
    }

    handleChangeValue(e) {
        const val = wktFromTypeAndData(this.state.type, e.target.value);
        if (val != this.props.item.value) {
            this.props.onChange({
                ...this.props.item,
                value: val
            });
        }
    }

    render() {
        const {item, disabled, onDelete} = this.props;
        const title = item.error && item.error.value ? item.error.value : '';
        const isPoint = this.state.type == 'POINT';
        return (
            <div className='reg-coordinates'>
                {
                    isPoint ?
                        <div className='value'>
                            <Button bsStyle='default' disabled>{wktType(this.state.type)}</Button>
                            <Input
                                type='text'
                                ref='focusEl'
                                className={title !== '' ? 'error' : ''}
                                title={title}
                                onFocus={this.props.onFocus}
                                onBlur={this.props.onBlur}
                                disabled={disabled}
                                onKeyUp={this.handleKeyUp}
                                onChange={this.handleChangeValue}
                                value={this.state.data}
                            />
                        </div>
                         : <div className='value-text'>
                        <Button bsStyle='default' disabled>{wktType(this.state.type)}</Button>
                        {i18n('subNodeForm.countOfCoordinates', this.state.data)}
                    </div>
                }
                <div className='description'>
                    <Input
                        type='string'
                        className='form-control'
                        disabled={disabled}
                        value={item.description}
                        onKeyUp={this.handleKeyUp}
                        onChange={this.handleChangeDescription}
                    />
                    {!isPoint ? <i className='fa fa-download download btn' onClick={this.props.onDownload} title={i18n('registry.coordinates.download')} /> : null}
                </div>
                <div>
                    <NoFocusButton disabled={disabled} onClick={onDelete}><Icon glyph='fa-times' /></NoFocusButton>
                </div>
            </div>
        )
    }
};

RegistryCoordinates.propTypes = {
    disabled: React.PropTypes.bool.isRequired,
    onChange: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onDownload: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func.isRequired,
    item: React.PropTypes.object.isRequired
};

module.exports = connect(null, null, null, { withRef: true })(RegistryCoordinates);