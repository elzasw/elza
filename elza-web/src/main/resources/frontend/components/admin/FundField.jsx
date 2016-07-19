// require("./FundField.less")

import React from "react";
import {WebApi} from "actions/index.jsx";
import {AbstractReactComponent, Autocomplete} from "components/index.jsx";
import {connect} from "react-redux"

const FundField = class FundField extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            "handleChange",
            "handleSearchChange",
            "focus");

        this.state = {
            dataList: []
        };
    }

    focus() {
        this.refs.autocomplete.focus()
    }

    componentDidMount() {
    }

    /**
     * Zajistíme vrácení onChange pouze objekt nebo null
     * @param id
     * @param valueObj
     */
    handleChange(id, valueObj) {
        this.props.onChange(valueObj.id ? valueObj : null);
    }

    handleSearchChange(text) {
        text = text == "" ? null : text;

        WebApi.findFunds(text).then(json => {
            this.setState({
                dataList: json.funds
            })
        })
    }

    render() {
        // onChange nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě
        const {value, onChange, ...otherProps} = this.props;
        const {dataList} = this.state;

        return (
            <Autocomplete
                ref="autocomplete"
                className="form-group"
                customFilter
                value={value}
                items={dataList}
                onSearchChange={this.handleSearchChange}
                onChange={this.handleChange}
                {...otherProps}
            />
        )
    }
}

FundField.propTypes = {
    value: React.PropTypes.object,
    onChange: React.PropTypes.func.isRequired,
    inline: React.PropTypes.bool,
    touched: React.PropTypes.bool,
    error: React.PropTypes.string,
}

function mapStateToProps(state) {
    return {
    }
}
module.exports = connect(mapStateToProps, null, null, { withRef: true })(FundField);
