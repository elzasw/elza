/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n, NoFocusButton, Icon, FormInput} from 'components/index.jsx';
import {objectFromWKT, wktFromTypeAndData, wktType} from 'components/Utils.jsx';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import {Button, OverlayTrigger, Tooltip} from 'react-bootstrap';
import DescItemLabel from './DescItemLabel.jsx'

require('./DescItemCoordinates.less');

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
        ReactDOM.findDOMNode(this.refs.uploadInput.refs.input).click();
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
        const {descItem, locked, repeatable, onUpload, readMode, cal} = this.props;
        let value = cal && descItem.value == null ? i18n("subNodeForm.descItemType.calculable") : descItem.value;

        if (readMode) {
            return (
                <DescItemLabel value={value} cal={cal} />
            )
        }

        const tooltip = <Tooltip id='tt'>{i18n('subNodeForm.formatPointCoordinates')}</Tooltip>;
        return (
            <div className="desc-item-value-coordinates">
                <div className='desc-item-value'  key='cords'>
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
                            : <span className="textvalue">{i18n('subNodeForm.countOfCoordinates', this.state.data)}</span>
                    }
                    { descItem.descItemObjectId && <div className='desc-item-coordinates-action' key='download-action'><NoFocusButton onClick={this.props.onDownload}>
                            <Icon glyph="fa-download"/>
                        </NoFocusButton></div>
                    }
                </div>
                {
                    !repeatable && <div className='desc-item-coordinates-action' key='cord-actions'>
                        <NoFocusButton onClick={this.handleUploadClick} title={i18n('subNodeForm.descItem.coordinates.action.add')}><Icon glyph="fa-upload" /></NoFocusButton>
                        <FormInput className="hidden" accept="application/vnd.google-earth.kml+xml" type="file" ref='uploadInput' onChange={onUpload} />
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

