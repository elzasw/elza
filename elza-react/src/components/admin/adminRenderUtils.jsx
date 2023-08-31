import React from 'react';

export function renderUserOrGroupItem(props) {
    const {item} = props;

    console.log(props);

    if (item) {
        if (item.user) {
            props.item = {item, ...item.user};
            return renderUserItem(props);
        } else if (item.group) {
            props.item = {item, ...item.group};
            return renderGroupItem(props);
        }
    }
}

/**
 * Returns name of given user in "preferredName(username)" format.
 *
 * @param {object} user
 * @return {string} name
 */
export function getUsername(user) {
    if (!user) {
        // if user does not exist
        throw new Error('Invalid user object');
    }
    if (user.preferredName) {
        // if user has preferredName defined
        return user.preferredName + ' (' + user.username + ')';
    } else if (user.accessPoint && user.accessPoint.name) {
        // if user has record name
        return user.accessPoint.name + ' (' + user.username + ')';
    } else {
        return user.username;
    }
}

export function renderUserOrGroupLabel(item) {
    if (item.user) {
        return getUsername(item.user);
    } else {
        return item.group.name;
    }
}

export function EntityItem({ item, getName, onClick, onMouseEnter }) {
    let name = 'unknown';

    if (getName) {
        name = getName(item);
    } else if (item && item.name) {
        name = item.name;
    }

    var fields = [];
    if (item.internalCode) {
        fields.push(item.internalCode);
    }
    if (item.fundNumber) {
        fields.push(item.fundNumber);
    }
    if (item.mark) {
        fields.push(item.mark);
    }
    var desc = fields.join(', ');

    return <div onClick={onClick} onMouseEnter={onMouseEnter} className="item" key={item.id}>
        <div className="item-row">
            <div className="name" title={name}>{name}</div>
        </div>
        <div className="item-row desc">
            <div>{desc}</div>
        </div>
    </div>

};

export function renderUserItem(props) {
    return (
        <EntityItem
            {...props}
            getName={item => {
                /*console.log("get username",item);*/
                return getUsername(item);
            }}
        />
    );
}

export function renderGroupItem(props) {
    return <EntityItem {...props} />;
}

export function renderFundItem(props) {
    return <EntityItem {...props} />;
}

export function renderScopeItem(props) {
    return <EntityItem {...props} />;
}
