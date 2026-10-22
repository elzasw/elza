import React from 'react';
import {connect} from 'react-redux';
import {contextMenuHide} from 'actions/global/contextMenu.jsx';
import AbstractReactComponent from '../../AbstractReactComponent';
import './ContextMenu.scss';

class ContextMenu extends AbstractReactComponent {
    componentDidMount() {
        document.addEventListener('mousedown', this.handleClick);
        document.addEventListener('touchstart', this.handleClick);
        window.addEventListener('resize', this.hideMenu);
        document.addEventListener('scroll', this.hideMenu);
    }

    componentDidUpdate() {
        this.fitIntoWindow();
    }

    componentWillUnmount() {
        document.removeEventListener('mousedown', this.handleClick);
        document.removeEventListener('touchstart', this.handleClick);
        window.removeEventListener('resize', this.hideMenu);
        document.removeEventListener('scroll', this.hideMenu);
    }

    /**
     * The menu opens at the cursor. When it does not fit below or to the right of the cursor,
     * it opens above or to the left of it instead, so that the whole menu stays in the window.
     */
    fitIntoWindow = () => {
        const {visible, position} = this.props;
        const menuNode = this.refs.contextMenu;
        if (!visible || !menuNode) {
            return;
        }

        const {offsetWidth, offsetHeight} = menuNode;
        const overflowsBottom = position.y + offsetHeight > window.innerHeight;
        const overflowsRight = position.x + offsetWidth > window.innerWidth;
        const top = overflowsBottom ? Math.max(0, position.y - offsetHeight) : position.y;
        const left = overflowsRight ? Math.max(0, position.x - offsetWidth) : position.x;
        menuNode.style.top = `${top}px`;
        menuNode.style.left = `${left}px`;
    };

    hideMenu = () => {
        this.props.dispatch(contextMenuHide());
    };

    handleClick = e => {
        let source = e.target;
        let found = false;
        let contextMenuDomNode = this.refs.contextMenu;

        while (source) {
            // console.log(source, contextMenuDomNode, source === contextMenuDomNode, source == contextMenuDomNode)
            found = source === contextMenuDomNode;
            if (found) {
                // console.log("FOUND...");
                return;
            }

            source = source.parentNode;
        }

        if (this.props.visible) {
            this.hideMenu();
        }
        // console.log("NOT FOUND...");
    };

    render() {
        if (!this.props.visible) {
            return <div ref="contextMenu" className="context-menu"></div>;
        }

        const style = {};
        style.top = this.props.position.y;
        style.left = this.props.position.x;

        return (
            <div ref="contextMenu" className="context-menu" style={style}>
                {this.props.menu}
            </div>
        );
    }
}

export default connect()(ContextMenu);
