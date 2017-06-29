import React from 'react';
import ReactDOM from 'react-dom';
import {TooltipTrigger, AbstractReactComponent, i18n, NoFocusButton, Icon, FormInput} from 'components/shared';
import {objectFromWKT, wktFromTypeAndData, wktType} from 'components/Utils.jsx';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import {Button, OverlayTrigger, Tooltip} from 'react-bootstrap';
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";

import './DescItemCoordinates.less';

/**
 * Input prvek pro desc item - typ STRING.
 */
class DescItemCoordinates extends AbstractReactComponent {

    constructor(props) {
        super(props);

        this.state = objectFromWKT(props.descItem.value);
    }

    static PropTypes = {
        onChange: React.PropTypes.func.isRequired,
        onDownload: React.PropTypes.func.isRequired,
        onUpload: React.PropTypes.func,
        descItem: React.PropTypes.object.isRequired,
        repeatable: React.PropTypes.bool.isRequired
    };

    componentWillReceiveProps(nextProps) {
        this.setState(objectFromWKT(nextProps.descItem.value));
    }

    focus = () => {
        this.refs.focusEl.focus()
    };

    handleUploadClick = () => {
        ReactDOM.findDOMNode(this.refs.uploadInput.refs.input).click();
    };

    handleChangeData = (e) => {
        const val = wktFromTypeAndData(this.state.type, e.target.value);
        if (val != this.props.descItem.value) {
            this.props.onChange(val);
        }
    };

    handleChangeSelect = (e) => {
        const val = wktFromTypeAndData(e.target.value, this.state.data);
        if (val != this.props.descItem.value) {
            this.props.onChange(val);
        }
    };

    render() {
        const {descItem, locked, repeatable, onUpload, readMode, cal} = this.props;
        let value = cal && descItem.value == null ? i18n("subNodeForm.descItemType.calculable") : descItem.value;

        if (readMode) {
            return <DescItemLabel value={value} cal={cal} />
        }

        return (
            <div className="desc-item-value-coordinates">
                <div className='desc-item-value'  key='cords'>
                    <Button bsStyle="default" disabled>{wktType(this.state.type)}</Button>
                    {
                        this.state.type == "POINT" ?
                            <ItemTooltipWrapper tooltipTitle="dataType.coordinates.format">
                                <input
                                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                                    ref='focusEl'
                                    disabled={locked}
                                    onChange={this.handleChangeData}
                                    value={this.state.data}
                                />
                            </ItemTooltipWrapper>
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
}

export default connect(null, null, null, {withRef: true})(DescItemCoordinates);

