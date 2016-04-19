/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, KmlMapDialog, i18n} from 'components';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'
import {Button} from 'react-bootstrap';

var DescItemCoordinates = class DescItemCoordinates extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChangeData','handleChangeSelect', 'focus', 'getStringType', 'getDownloadUrl');

        this.state = this.parseData(props.descItem.value);
    }

    parseData(value) {
        if (value === null) {
            return {type: "POINT", data:null};
        }
        const state = {type: null, data: null};
        const start = value.indexOf("(");
        state.type = value.substr(0, start).trim();
        if (state.type === "POINT") {
            state.data = value.substr(start+1,value.length-start-2).split(", ").join("\n").split(" ").join(",");
        } else {
            state.data = value.substr(start+2,value.length-start-4);
        }
        return state;
    }

    componentWillReceiveProps(nextProps) {
        this.setState(this.parseData(nextProps.descItem.value))
    }

    focus() {
        this.refs.focusEl.focus()
    }

    handleChangeData(e) {
        const val = this.getDataFormatted(this.state.type, e.target.value);
        if (val != this.props.descItem.value) {
            this.props.onChange(val);
        }
    }

    getDataFormatted(type, val) {
        var points = val.split(",").join(" ").split("\n").join(", ");
        if (type === "POLYGON") {
            points = "(" + points + ", " + points.substr(0, points.indexOf(",")) + ")";
        }
        return type + "(" + points + ")";
    }

    handleChangeSelect(e) {
        const val = this.getDataFormatted(e.target.value, this.state.data);
        if (val != this.props.descItem.value) {
            this.props.onChange(val);
        }
    }

    getStringType() {
        switch (this.state.type) {
            case "POINT":
                return "B";
            case "POLYGON":
                return "P";
            case "LINESTRING":
                return "L";
            default:
                return "N";
        }
    }

    getDownloadUrl() {
        return window.location.origin + "/api/kmlManagerV1/" + this.props.descItem.descItemObjectId + "/" + this.props.fundVersionId + "/exportDescItemCoordinates";
    }

    render() {
        const {descItem, locked} = this.props;
        return (
            <div >
                <div className='desc-item-value  desc-item-value-parts'>
                    <Button bsStyle="default" disabled>{this.getStringType()}</Button>
                    {
                        this.state.type == "POINT" ?
                            <input
                                {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                                ref='focusEl'
                                disabled={locked}
                                onChange={this.handleChangeData.bind(this)}
                                value={this.state.data}
                            />: <div>
                            <span>{i18n('subNodeForm.countOfCoordinates', this.state.data)}</span>
                            <Button bsStyle="default" href={this.getDownloadUrl()}><i className="fa fa-download" /></Button>
                        </div>
                    }
                </div>
            </div>
        )
    }
};

function mapStateToProps(state) {
    const {arrRegion} = state;
    var fundVersionId = null;
    if (arrRegion.activeIndex != null) {
        fundVersionId = arrRegion.funds[arrRegion.activeIndex].versionId;
    }

    return {
        fundVersionId: fundVersionId
    }
}
module.exports = connect(mapStateToProps, null, null, { withRef: true })(DescItemCoordinates);

