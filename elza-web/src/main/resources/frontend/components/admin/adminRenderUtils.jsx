import React from "react";

export function renderUserItem(user, isHighlighted = false, isSelected = false) {
    var cls = 'item';
    if (isHighlighted) {
        cls += ' focus'
    }
    if (isSelected) {
        cls += ' active'
    }

    var itemStr = user.party.record.record + " (" + user.username + ")";
    return (
        <div
            className={cls}
            key={user.id}
        >{itemStr}</div>
    )
}
export function renderGroupItem(group, isHighlighted = false, isSelected = false) {
    var cls = 'item';
    if (isHighlighted) {
        cls += ' focus'
    }
    if (isSelected) {
        cls += ' active'
    }

    var itemStr = group.name;
    return (
        <div
            className={cls}
            key={group.id}
        >{itemStr}</div>
    )
}