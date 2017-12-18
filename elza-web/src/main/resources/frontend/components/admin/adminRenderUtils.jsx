import React from "react";

export function renderUserOrGroupItem(item, isHighlighted = false, isSelected = false) {
    if (item.user) {
        return renderUserItem(item.user, isHighlighted, isSelected);
    } else {
        return renderGroupItem(item.group, isHighlighted, isSelected);
    }
}
/**
 * Returns name of given user in "preferredName(username)" format.
 *
 * @param {object} user
 * @return {string} name
 */
export function getUsername(user){
    if(!user){
        // if user does not exist
        throw new Error("Invalid user object");
    }
    if(user.preferredName){
        // if user has preferredName defined
        return user.preferredName + " (" + user.username + ")";
    } else if (user.party && user.party.record && user.party.record.record) {
        // if user has record name
        return user.party.record.record + " (" + user.username + ")";
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

export function renderItem(id, itemStr, isHighlighted = false, isSelected = false) {
    let cls = 'item';
    if (isHighlighted) {
        cls += ' focus'
    }
    if (isSelected) {
        cls += ' active'
    }

    return (
        <div
            className={cls}
            key={id}
        >{itemStr}</div>
    )
}

export function renderUserItem(user, isHighlighted = false, isSelected = false) {
    const itemStr = getUsername(user);
    return renderItem(user.id, itemStr, isHighlighted, isSelected);
}

export function renderGroupItem(group, isHighlighted = false, isSelected = false) {
    const itemStr = group.name;
    return renderItem(group.id, itemStr, isHighlighted, isSelected);
}

export function renderFundItem(fund, isHighlighted = false, isSelected = false) {
    const itemStr = fund.name;
    return renderItem(fund.id, itemStr, isHighlighted, isSelected);
}

export function renderScopeItem(scope, isHighlighted = false, isSelected = false) {
    const itemStr = scope.name;
    return renderItem(scope.id, itemStr, isHighlighted, isSelected);
}
