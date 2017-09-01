import React from "react";
import {WebApi} from "actions/index.jsx";
import {AbstractReactComponent, Autocomplete} from 'components/shared';
import {connect} from "react-redux"
import {renderUserItem} from "./adminRenderUtils.jsx"

const UserField = class UserField extends AbstractReactComponent {
    static defaultProps = {
        tags: false,
        excludedGroupId: null
    };

    static PropTypes = {
        value: React.PropTypes.object,
        onChange: React.PropTypes.func.isRequired,
        inline: React.PropTypes.bool,
        touched: React.PropTypes.bool,
        error: React.PropTypes.string,
        tags: React.PropTypes.bool,
        excludedGroupId: React.PropTypes.number,
    };

    constructor(props) {
        super(props);
        this.bindMethods(
            "handleChange",
            "handleSearchChange",
            "focus"
        );

        this.state = {
            dataList: []
        };
    }

    focus() {
        this.refs.autocomplete.focus()
    }

    handleSearchChange(text) {
        text = text == "" ? null : text;

        WebApi.findUser(text, true, false, 200, this.props.excludedGroupId).then(json => {
            this.setState({
                dataList: json.users
            })
        })
    }

    render() {
        // onChange nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě
        const {tags, value, ...otherProps} = this.props;
        const {dataList} = this.state;

        return (
            <Autocomplete
                tags={tags}
                ref="autocomplete"
                className="form-group"
                customFilter
                value={value}
                items={dataList}
                onSearchChange={this.handleSearchChange}
                {...otherProps}
                renderItem={renderUserItem}
            />
        )
    }
}

export default connect(null, null, null, { withRef: true })(UserField);
