require ('./ContextMenu.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent} from 'components/index.jsx';
import {contextMenuHide} from 'actions/global/contextMenu.jsx'

var ContextMenu = class extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('hideMenu', 'handleClick');
    }

    componentDidMount() {
        this.contextMenuDomNode = ReactDOM.findDOMNode(this.refs.contextMenu);

        document.addEventListener("mousedown", this.handleClick);
        document.addEventListener("touchstart", this.handleClick);
        window.addEventListener("resize", this.hideMenu);
        document.addEventListener("scroll", this.hideMenu);        
    }

    componentWillUnmount() {
        document.removeEventListener("mousedown", this.handleClick);
        document.removeEventListener("touchstart", this.handleClick);
        window.removeEventListener("resize", this.hideMenu);
        document.removeEventListener("scroll", this.hideMenu);
    }

    hideMenu() {
        this.dispatch(contextMenuHide());
    }

    handleClick(e) {
        var source = e.target;
        var found = false;

        while (source.parentNode) {
            found = (source === this.contextMenuDomNode);
            if (found) {
                //console.log("FOUND...");
                return
            }

            source = source.parentNode;
        }

        if (this.props.visible) {
            this.hideMenu();
        }
        //console.log("NOT FOUND...");
    }

    render() {
        if (!this.props.visible) {
            return <div ref="contextMenu" className='context-menu'></div>;
        }

        var style = {};
        style.top = this.props.position.y;
        style.left = this.props.position.x;

        return (
            <div ref="contextMenu" className='context-menu' style={style}>
                {this.props.menu}
            </div>
        )
    }
}

module.exports = connect()(ContextMenu);