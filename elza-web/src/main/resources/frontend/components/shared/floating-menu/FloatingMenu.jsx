import React from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import {getScrollbarWidth} from "components/Utils.jsx";
import classNames from "classnames";
import './FloatingMenu.less';

export default class FloatingMenu extends React.PureComponent {
    static propTypes = {
        target: PropTypes.object,
        coordinates: PropTypes.object,
        shouldUpdate: PropTypes.bool,
        focusable: PropTypes.bool,
        closeMenu: PropTypes.func,
        //@TODO: selectable placement direction
        //position: PropTypes.oneOf(["both", "horizontal", "vertical", "left", "right", "up", "down"])
    }

    static defaultProps = {
        shouldUpdate: true,
        closeMenu: ()=>{},
        coordinates: {x:0,y:0}
    }

    constructor(props) {
        super(props);
        this.state = {};
    }

    componentDidMount = () => {
        document.addEventListener("mousedown", this.handleDocumentClick, false);
        this.setMenuPositions();
    }

    componentWillUnmount = () => {
        document.removeEventListener("mousedown", this.handleDocumentClick, false);
    }

    componentDidUpdate = (prevProps, prevState) => {
         if(this.props.shouldUpdate){
             this.setMenuPositions();
         }
    }

    handleDocumentClick = (e) => {
        const {closeMenu} = this.props;
        // element relative to which the menu is placed
        const origin = ReactDOM.findDOMNode(this.props.target);
        // the menu element
        const menu = ReactDOM.findDOMNode(this.menu);
        let eventTarget = e.target;
        let inside = false;

        // looks whether the click was made inside of the menu
        // (goes through parent nodes of the event target)
        while (eventTarget !== null) {
            // stops if the event target or its parents are either the origin element
            // or the menu element
            if (eventTarget === origin || eventTarget === menu) {
                return;
            }
            eventTarget = eventTarget.parentNode;
        }

        // call the closeMenu callback, if the click was made outside of the menu
        closeMenu();
    }

    /**
     * Získá maximální a minimální rozměry elementu vůči origin elementu a okrajům obrazovky
     * @param {object} origin
     * @return {object}
     */
    getElementRelativeScreenConstraints = (originRect) => {
        const screen = $(document);
        const originOffset = this.getRectScreenOffset(originRect);
        var maxHeight = originOffset.bottom < originOffset.top ? originOffset.top : originOffset.bottom;
        var maxWidth = screen.width();
        var minWidth = originRect.width;
        var minHeight = 0;
        return{
            minHeight: minHeight,
            maxHeight: maxHeight,
            minWidth: minWidth,
            maxWidth: maxWidth
        };
    }

    /**
     * Získá rozměry elementu vzhledem k velikostním omezením
     * @param {object} element
     * @param {object} constraints
     * @return {object}
     */
    getConstrainedElementSize = (element, constraints) => {
        const {maxWidth, maxHeight, minWidth, minHeight} = constraints;
        let scrollbarWidth = getScrollbarWidth();
        let height = element.getBoundingClientRect().height;
        let width = element.getBoundingClientRect().width;

        const heightLimited = height > maxHeight;
        const heightStretched = height < minHeight;

        if(heightLimited){
            height = maxHeight;
        } else if(heightStretched){
            height = minHeight;
        }

        const widthLimited = width > maxWidth;
        const widthStretched = width < minWidth;
        const willLineBreak = width >= minWidth - scrollbarWidth;

        if(heightLimited && !widthLimited && willLineBreak){
            width = width + scrollbarWidth;
        } else if(widthLimited){
            width = maxWidth;
        } else if (widthStretched){
            width = minWidth;
        }

        return {
            width,
            height
        };
    }

    /**
     * Získá odsazení od okrajů stránky
     * @param {object} nodeRect
     * @return {object}
     */
    getRectScreenOffset = (nodeRect) => {
        const screen = $(document);
        return {
            top: nodeRect.top,
            bottom: screen.height() - nodeRect.bottom,
            left: nodeRect.left,
            right: screen.width() - nodeRect.right
        };
    }

    /**
     * Nastaví automatickou velikost elementu
     * @param {object} element
     */
    resetElementSize = (element) => {
        $(element).css({height: "auto",width: "auto"});
    }

    /**
     * Vrací objekt s umístěním našeptávače
     * @param {object} origin
     * @return {object}
     */
    getRelativeMenuPlacement = (rect, size) => {
        const originOffset = this.getRectScreenOffset(rect);
        var placement = {};

        if(size.width > (originOffset.right + rect.width)){
            placement.right = 0;
        } else {
            placement.left = originOffset.left + "px";
        }
        if (originOffset.bottom < originOffset.top) { // nevejde se dolu, dáme ho nahoru
            placement.bottom = originOffset.bottom + rect.height + 'px';
        } else {
            placement.top = rect.bottom + 'px';
        }
        return placement;
    }

    getRectFromCoordinates = (x, y) => {
         return {
            left: x,
            right: x,
            top: y,
            bottom: y,
            width: 0,
            height: 0
        }
    }

    setMenuPositions() {
        const {coordinates, focusable} = this.props;
        const targetNode = ReactDOM.findDOMNode(this.props.target);
        const containerNode = ReactDOM.findDOMNode(this.menu);
        let originRect = this.getRectFromCoordinates(coordinates.x, coordinates.y);
        if(targetNode){
            originRect = targetNode.getBoundingClientRect();
        }

        //Resetování velikostí elementů
        this.resetElementSize(containerNode);

        //Zjistí velikost obsahu
        var constraints = this.getElementRelativeScreenConstraints(originRect);
        var size = this.getConstrainedElementSize(containerNode,constraints);
        //Zjištění okrajů okna našeptávače
        var placement = this.getRelativeMenuPlacement(originRect, size);

        $(containerNode).css({
            height: size.height + 'px',
            width: size.width + 'px',
            ...placement
        });
        if (focusable && this.menu.children && this.menu.children[0]) {
            this.menu.children[0].focus();
        }
        //this.setState({...this.state});
    }

    render() {
        const {children, onMouseDown, onMouseUp} = this.props;
        const {style} = this.state;

        let cls = classNames({
            "floating-menu": true,
            "active": true
        });

        return (
            <div ref={(ref)=>{this.menu = ref;}} className={cls} onMouseDown={onMouseDown} onMouseUp={onMouseUp}>
              {children}
            </div>
        );
    }
}
