// require("./ScopeField.less")

import React from "react";
import {AbstractReactComponent, Autocomplete} from 'components/shared';
import {connect} from "react-redux"

const ScopeField = class ScopeField extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            "handleChange",
            "focus");
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
    handleChange(valueObj) {
        this.props.onChange(valueObj.id ? valueObj : null);
    }

    render() {
        // onChange nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě
        const {value, onChange, scopes, ...otherProps} = this.props;

        return (
            <Autocomplete
                ref="autocomplete"
                className="form-group"
                value={value}
                items={scopes}
                onChange={this.handleChange}
                {...otherProps}
            />
        )
    }
}

ScopeField.propTypes = {
    value: React.PropTypes.object,
    onChange: React.PropTypes.func.isRequired,
    scopes: React.PropTypes.array.isRequired,
    inline: React.PropTypes.bool,
    touched: React.PropTypes.bool,
    error: React.PropTypes.string,
}

function mapStateToProps(state) {
    return {
    }
}
export default connect(mapStateToProps, null, null, { withRef: true })(ScopeField);
