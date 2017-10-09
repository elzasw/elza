// ---
import React from "react";
import {WebApi} from "actions/index.jsx";
import {AbstractReactComponent, Autocomplete, Utils} from 'components/shared';
import {connect} from "react-redux"
import {renderUserOrGroupItem} from "./adminRenderUtils.jsx"

/**
 * Field pro vybrání skupiny nebo uživatele.
 */
class UserAndGroupField extends AbstractReactComponent {
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
    };

    constructor(props) {
        super(props);

        this.state = {
            dataList: []
        };
    }

    focus = () => {
        this.refs.autocomplete.focus();
    };

    handleSearchChange = (text) => {
        text = text == "" ? null : text;

        const findUser = WebApi.findUser(text, true, false, 200, null);
        const findGroup = WebApi.findGroup(text);

        Utils.barrier(findUser, findGroup)
            .then(data => ({users: data[0].data.users, groups: data[1].data.groups}))
            .then(data => {
                const rows = [];
                data.users.forEach(u => {
                    rows.push({
                        id: `u-${u.id}`,
                        user: u
                    });
                });
                data.groups.forEach(g => {
                    rows.push({
                        id: `g-${g.id}`,
                        group: g
                    });
                });

                this.setState({
                    dataList: rows
                })
            });
    };

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
                renderItem={renderUserOrGroupItem}
            />
        )
    }
}

export default connect(null, null, null, { withRef: true })(UserAndGroupField);
