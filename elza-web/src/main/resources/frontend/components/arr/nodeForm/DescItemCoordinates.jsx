/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n} from 'components';
import {objectFromWKT, wktFromTypeAndData, wktType} from 'components/Utils';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'
import {Button} from 'react-bootstrap';

var DescItemCoordinates = class DescItemCoordinates extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChangeData', 'handleChangeSelect', 'focus');

        this.state = objectFromWKT(props.descItem.value);
    }

    componentWillReceiveProps(nextProps) {
        this.setState(objectFromWKT(nextProps.descItem.value));
    }

    focus() {
        this.refs.focusEl.focus()
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
        const {descItem, locked} = this.props;
        return (
            <div>
                <div className='desc-item-value  desc-item-value-parts'>
                    <Button bsStyle="default" disabled>{wktType(this.state.type)}</Button>
                    {
                        this.state.type == "POINT" ?
                            <input
                                {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                                ref='focusEl'
                                disabled={locked}
                                onChange={this.handleChangeData}
                                value={this.state.data}
                            /> : <div>
                            <span>{i18n('subNodeForm.countOfCoordinates', this.state.data)}</span>
                            <Button bsStyle="default" onClick={this.props.onDownload}>
                                <i className="fa fa-download"/>
                            </Button>
                        </div>
                    }
                </div>
            </div>
        )
    }
};

DescItemCoordinates.propTypes = {
    onChange: React.PropTypes.func.isRequired,
    onDownload: React.PropTypes.func.isRequired,
    descItem: React.PropTypes.object.isRequired
};

module.exports = connect(null, null, null, {withRef: true})(DescItemCoordinates);

