import React from 'react';
import ReactDOM from 'react-dom';
import {Button, Tooltip, OverlayTrigger} from 'react-bootstrap';
import {objectFromWKT, wktFromTypeAndData, wktType} from 'components/Utils.jsx';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, NoFocusButton, Icon, FormInput} from 'components/index.jsx';
import './RegistryCoordinates.less';

class RegistryCoordinates extends AbstractReactComponent {
    static PropTypes = {
        disabled: React.PropTypes.bool.isRequired,
        onChange: React.PropTypes.func.isRequired,
        onDelete: React.PropTypes.func.isRequired,
        onDownload: React.PropTypes.func.isRequired,
        onEnterKey: React.PropTypes.func.isRequired,
        item: React.PropTypes.object.isRequired
    };

    constructor(props) {
        super(props);
        this.state = objectFromWKT(props.item.value);
    }

    componentWillReceiveProps(nextProps) {
        this.setState(objectFromWKT(nextProps.item.value));
    }

    focus = () => {
        this.refs.focusEl.focus()
    };

    handleKeyUp = (e) => {
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
    };

    handleChangeDescription = (e) => {
        const val = e.target.value;
        if (val != this.props.item.description) {
            this.props.onChange({
                ...this.props.item,
                description: val
            });
        }
    };

    handleChangeValue = (e) => {
        const val = wktFromTypeAndData(this.state.type, e.target.value);
        if (val != this.props.item.value) {
            this.props.onChange({
                ...this.props.item,
                value: val
            });
        }
    };

    render() {
        const {item, disabled, onDelete, onBlur} = this.props;
        const title = item.error && item.error.value ? item.error.value : '';
        const isPoint = this.state.type == 'POINT';
        const tooltip = <Tooltip id='ttf'>{i18n('registry.coordinates.format')}</Tooltip>;
        return (
            <div className='reg-coordinates'>
                {
                    isPoint ?
                        <div className='value'>
                            <Button bsStyle='default' disabled>{wktType(this.state.type)}</Button>
                            <OverlayTrigger overlay={tooltip} placement="bottom">
                                <FormInput
                                    type='text'
                                    ref='focusEl'
                                    className={title !== '' ? 'error' : ''}
                                    title={title}
                                    onFocus={this.props.onFocus}
                                    onBlur={onBlur}
                                    disabled={disabled}
                                    onKeyUp={this.handleKeyUp}
                                    onChange={this.handleChangeValue}
                                    value={this.state.data}
                                />
                            </OverlayTrigger>
                            {item.id ? <i className='fa fa-download download btn' onClick={this.props.onDownload} title={i18n('registry.coordinates.download')} /> : null}
                        </div>
                         : <div className='value-text'>
                            <Button bsStyle='default' disabled>{wktType(this.state.type)}</Button>
                            {i18n('subNodeForm.countOfCoordinates', this.state.data)}
                            {item.id ? <i className='fa fa-download download btn' onClick={this.props.onDownload} title={i18n('registry.coordinates.download')} /> : null}
                        </div>
                }
                <div className='description'>
                    <FormInput
                        type='text'
                        ref='focusEldesc'
                        disabled={disabled}
                        onBlur={onBlur}
                        value={item.description}
                        onKeyUp={this.handleKeyUp}
                        onChange={this.handleChangeDescription}
                    />
                </div>
                <div>
                    <NoFocusButton disabled={disabled} onClick={onDelete}><Icon glyph='fa-times' /></NoFocusButton>
                </div>
            </div>
        )
    }
}

export default connect(null, null, null, { withRef: true })(RegistryCoordinates);