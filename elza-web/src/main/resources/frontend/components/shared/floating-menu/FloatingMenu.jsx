import React from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import {getScrollbarWidth} from "components/Utils.jsx";
import classNames from "classnames";
import './FloatingMenu.less';

export default class FloatingMenu extends React.PureComponent {
    static propTypes = {
        target: PropTypes.object.isRequired,
        shouldUpdate: PropTypes.bool,
        closeMenu: PropTypes.func
    }

    static defaultProps = {
        shouldUpdate: true,
        closeMenu: ()=>{}
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

        // if the click was made outside of the menu,
        // call the closeMenu callback (if it exists)
        closeMenu && closeMenu();
    }

    /**
     * Získá maximální a minimální rozměry elementu vůči origin elementu a okrajům obrazovky
     * @param {object} origin
     * @return {object}
     */
    getElementRelativeScreenConstraints = (origin) => {
        const originRect = origin.getBoundingClientRect();
        const originOffset = this.getRectScreenOffset(originRect);
        var maxHeight = originOffset.bottom < originOffset.top ? originOffset.top : originOffset.bottom;
        var maxWidth = originOffset.right + originRect.width;
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
     * Získá maximální a minimální rozměry elementu vůči origin elementu a okrajům obrazovky.
     * Bere v úvahu i omezení z css stylů daného elementu
     * @param {object} node
     * @param {object} origin
     * @return {object}
     */
    getSizeConstraints = (node, origin) => {
        const nodeStyle = getComputedStyle(node);
        var constraints = this.getElementRelativeScreenConstraints(origin);
        // Pokud má obalující element našeptávače nastavenou maximální výšku nebo šířku,
        // která je menší než maximální povolená hodnota, přiřadí se její hodnota
        var maxWidth = nodeStyle.maxWidth === 'none' || (parseInt(nodeStyle.maxWidth , 10) > constraints.maxWidth) ? constraints.maxWidth : parseInt(nodeStyle.maxWidth , 10);
        var maxHeight = nodeStyle.maxHeight === 'none' || (parseInt(nodeStyle.maxHeight , 10) > constraints.maxHeight) ? constraints.maxHeight : parseInt(nodeStyle.maxHeight , 10);
        var minWidth = nodeStyle.minWidth === 'none' || (parseInt(nodeStyle.minWidth , 10) < constraints.minWidth) ? constraints.minWidth : parseInt(nodeStyle.minWidth , 10);
        var minHeight = nodeStyle.minHeight === 'none' || (parseInt(nodeStyle.minHeight , 10) < constraints.minHeight) ? constraints.minHeight : parseInt(nodeStyle.minHeight , 10);
        return {
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
        let height = element.offsetHeight;
        let width = element.offsetWidth;

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
    getRelativeMenuPlacement = (origin) => {
        const originRect = origin.getBoundingClientRect();
        const originOffset = this.getRectScreenOffset(originRect);
        var placement = {left: originOffset.left + 'px'};
        if (originOffset.bottom < originOffset.top) { // nevejde se dolu, dáme ho nahoru
            placement.bottom = originOffset.bottom + originRect.height + 'px';
        } else {
            placement.top = originRect.bottom + 'px';
        }
        return placement;
    }

    setMenuPositions() {
        const targetNode = ReactDOM.findDOMNode(this.props.target);
        const containerNode = ReactDOM.findDOMNode(this.menu);

        //Resetování velikostí elementů
        this.resetElementSize(containerNode);

        //Zjistí velikost obsahu
        var containerConstraints = this.getSizeConstraints(containerNode,targetNode);
        var containerSize = this.getConstrainedElementSize(containerNode,containerConstraints);
        //Zjištění okrajů okna našeptávače
        var containerPlacement = this.getRelativeMenuPlacement(targetNode);

        $(containerNode).css({
            height: containerSize.height + 'px',
            width: containerSize.width + 'px',
            ...containerPlacement
        });
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
