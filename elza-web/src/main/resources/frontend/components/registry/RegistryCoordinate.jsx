import React from 'react';
import ReactDOM from 'react-dom';
import {Button} from 'react-bootstrap';
import {objectFromWKT, wktFromTypeAndData, wktType} from 'components/Utils.jsx';
import {connect} from 'react-redux'
import {TooltipTrigger, AbstractReactComponent, i18n, NoFocusButton, Icon, FormInput} from 'components/index.jsx';
import './RegistryCoordinate.less';

class RegistryCoordinate extends AbstractReactComponent {
    static PropTypes = {
        disabled: React.PropTypes.bool.isRequired,
        onChange: React.PropTypes.func.isRequired,
        onDelete: React.PropTypes.func.isRequired,
        onDownload: React.PropTypes.func.isRequired,
        onEnterKey: React.PropTypes.func.isRequired,
        item: React.PropTypes.object.isRequired
    };


    state = objectFromWKT(this.props.item.value);

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
        const {item, disabled, onDelete, onBlur, onFocus, onDownload} = this.props;
        const title = item.error && item.error.value ? item.error.value : '';
        const isPoint = this.state.type == 'POINT';

        const tooltipText = i18n("^dataType.coordinates.format");
        const tooltip = tooltipText ? <div dangerouslySetInnerHTML={{__html: tooltipText}}></div> : null;

        return <div className='reg-coordinate'>
            {
                isPoint ?
                    <div className='value'>
                        <Button bsStyle='default' disabled>{wktType(this.state.type)}</Button>
                        <TooltipTrigger
                            content={tooltip}
                            holdOnHover
                            placement="vertical"
                            className="input-container"
                        >
                            <FormInput
                                type='text'
                                ref='focusEl'
                                className={title !== '' ? 'error' : ''}
                                title={title}
                                onFocus={onFocus}
                                onBlur={onBlur}
                                disabled={disabled}
                                onKeyUp={this.handleKeyUp}
                                onChange={this.handleChangeValue}
                                value={this.state.data}
                            />
                        </TooltipTrigger>
                        {item.id && <NoFocusButton onClick={onDownload}><i className='fa fa-download download btn'  title={i18n('registry.coordinates.download')} /></NoFocusButton>}
                    </div>
                     : <div className='value-text'>
                        <Button bsStyle='default' disabled>{wktType(this.state.type)}</Button>
                        {i18n('subNodeForm.countOfCoordinates', this.state.data)}
                        {item.id && <NoFocusButton onClick={onDownload}><i className='fa fa-download download btn' title={i18n('registry.coordinates.download')} /></NoFocusButton>}
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
    }
}

export default RegistryCoordinate;
