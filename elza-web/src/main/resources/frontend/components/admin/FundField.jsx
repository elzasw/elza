import React from "react";
import {WebApi} from "actions/index.jsx";
import {AbstractReactComponent, Autocomplete} from "components/index.jsx";
import {connect} from "react-redux";

// import "./FundField.less"

class FundField extends AbstractReactComponent {

    static PropTypes = {
        value: React.PropTypes.object,
        onChange: React.PropTypes.func.isRequired,
        inline: React.PropTypes.bool,
        touched: React.PropTypes.bool,
        error: React.PropTypes.string,
    };

    state = {
        dataList: []
    };

    focus = () => {
        this.refs.autocomplete.focus()
    };

    handleSearchChange = (text) => {
        text = text == "" ? null : text;

        WebApi.findFunds(text).then(json => {
            this.setState({
                dataList: json.funds
            })
        })
    };

    render() {
        // onChange nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě
        const {value, ...otherProps} = this.props;
        const {dataList} = this.state;

        return <Autocomplete
            ref="autocomplete"
            className="form-group"
            customFilter
            value={value}
            items={dataList}
            onSearchChange={this.handleSearchChange}
            {...otherProps}
        />;
    }
}

export default connect(null, null, null, { withRef: true })(FundField);
