/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n, NoFocusButton, Icon} from 'components/index.jsx';
import {objectFromWKT, wktFromTypeAndData, wktType} from 'components/Utils.jsx';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import {Button, Input, OverlayTrigger, Tooltip} from 'react-bootstrap';

var DescItemCoordinates = class DescItemCoordinates extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChangeData', 'handleChangeSelect', 'focus', 'handleUploadClick');

        this.state = objectFromWKT(props.descItem.value);
    }

    componentWillReceiveProps(nextProps) {
        this.setState(objectFromWKT(nextProps.descItem.value));
    }

    focus() {
        this.refs.focusEl.focus()
    }

    handleUploadClick() {
        this.refs.uploadInput.getInputDOMNode().click();
    }

    handleChangeData(e) {
        const val = wktFromTypeAndData(this.state.type, e.target.value);
        if (val != this.props.descItem.value) {
            this.props.onChange(val);
        }
    }

    handleChangeSelect(e) {
        const val = wktFromTypeAndData(e.target.value, this.state.data);
        if (val != this.props.descItem.value) {
            this.props.onChange(val);
        }
    }

    render() {
        const {descItem, locked, repeatable, onUpload} = this.props;
        const tooltip = <Tooltip id='tt'>{i18n('subNodeForm.formatPointCoordinates')}</Tooltip>;
        return (
            <div>
                <div className='desc-item-value  desc-item-value-parts'  key='cords'>
                    <Button bsStyle="default" disabled>{wktType(this.state.type)}</Button>
                    {
                        this.state.type == "POINT" ?
                            <OverlayTrigger overlay={tooltip} placement="bottom">
                                <input
                                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                                    ref='focusEl'
                                    disabled={locked}
                                    onChange={this.handleChangeData}
                                    value={this.state.data}
                                />
                            </OverlayTrigger>
                            : <span>{i18n('subNodeForm.countOfCoordinates', this.state.data)}</span>
                    }
                    { descItem.descItemObjectId && <NoFocusButton bsStyle="default" onClick={this.props.onDownload}>
                            <i className="fa fa-download"/>
                        </NoFocusButton>
                    }
                </div>
                {
                    !repeatable && <div className='desc-item-type-actions' key='cord-actions'>
                        <NoFocusButton onClick={this.handleUploadClick} title={i18n('subNodeForm.descItemType.title.add')}><Icon glyph="fa-upload" /></NoFocusButton>
                        <Input className="hidden" accept="application/vnd.google-earth.kml+xml" type="file" ref='uploadInput' onChange={onUpload} />
                    </div>
                }
            </div>
        )
    }
};

DescItemCoordinates.propTypes = {
    onChange: React.PropTypes.func.isRequired,
    onDownload: React.PropTypes.func.isRequired,
    onUpload: React.PropTypes.func,
    descItem: React.PropTypes.object.isRequired,
    repeatable: React.PropTypes.bool.isRequired
};

module.exports = connect(null, null, null, {withRef: true})(DescItemCoordinates);

