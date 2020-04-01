/**
 *  Panel záložek
 *
 *  Pro inicializaci staci naimportovat: import {Tabs} from 'components/index.jsx';
 *
 **/

import React from 'react';
import {Nav} from 'react-bootstrap';
import * as Utils from '../../Utils';
import {Shortcuts} from 'react-shortcuts';
import {PropTypes} from 'prop-types';
import defaultKeymap from './TabsKeymap.jsx';

import './Tabs.scss';
import Icon from '../icon/Icon';
import NoFocusButton from '../button/NoFocusButton';
import i18n from '../../i18n';

/**
 *  Obalovací komponenta pro záložky a jejich obsah
 *  @param string className             třída komponenty
 *  $param obj children                 vnitřní části komponenty (přepínací panel, data zobrazovaná pod záložkami)
 **/
export const Container = class TabsContainer extends React.Component {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    handleShortcuts = (action, e) => {
        e.stopPropagation();
        e.preventDefault();

        let tabs;
        for (let a = 0; a < this.props.children.length; a++) {
            if (this.props.children[a].type === Tabs) {
                tabs = this.props.children[a];
                break;
            }
        }
        if (!tabs) {
            console.error(
                'First child of TabsContainer must be Tabs component!',
                'Existing children',
                this.props.children,
            );
            return;
        }

        const {activeItem, items, onSelect} = tabs.props;

        if (items.length === 0) {
            return;
        }

        let index = 0;
        for (let a = 0; a < items.length; a++) {
            if (items[a] === activeItem) {
                index = a;
            }
        }

        switch (action) {
            case 'prevTab':
                if (index > 0) {
                    onSelect(items[index - 1]);
                } else {
                    onSelect(items[items.length - 1]);
                }
                break;
            case 'nextTab':
                if (index + 1 < items.length) {
                    onSelect(items[index + 1]);
                } else {
                    onSelect(items[0]);
                }
                break;
            default:
                return;
        }
    };

    render() {
        // metoda pro renderovani obsahu komponenty
        let cls = 'tabs-container'; // třída komponenty
        if (this.props.className) {
            cls += ' ' + this.props.className;
        }
        return (
            <Shortcuts className={cls} name="Tabs" handler={this.handleShortcuts} stopPropagation={false}>
                {this.props.children}
            </Shortcuts>
        );
    }
};

/**
 *  Komponena pro zobrazeni obshu dané záložky
 *  @param string className     třída komponenty
 *  $param obj children         datový obsah komponenty
 **/
export const Content = class TabContent extends React.Component {
    render() {
        let cls = 'tab-content'; // třída komponenty
        if (this.props.className) {
            cls += ' ' + this.props.className;
        }

        const key = this.props.key || 'tab-content';

        return (
            <div key={key} className={cls}>
                {this.props.children}
            </div>
        );
    }
};

/**
 *  Komponena pro panelů záložek pro přepínání
 *  @param string className             // třída komponenty
 *  $param obj children                 // vnitřní části komponenty (přepínací panel, data zobrazovaná pod záložkami)
 **/
export const Tabs = class Tabs extends React.Component {
    /**
     *  Zavření vybrané záložky
     *  @ param obj item        objekt záložky
     *  @ param event e         událost kliknutí na ikonu zavření záložky
     **/
    handleTabClose = (item, e) => {
        this.props.onClose(item); // zavření záložky

        e.preventDefault(); // zastavení původní akce vyvolané událostí
        e.stopPropagation(); // zastavení dalších akcí vázaných na tuto událost
    };

    /**
     *  Zobrazení obsahu vybrané záložky
     *  @ param int itemID      lokální identidikátor záložky
     **/
    handleTabSelect = itemKey => {
        if (this.props.asTabs) {
            this.props.onSelect(itemKey);
        } else {
            let item = this.props.items.one(i => {
                let key = typeof i.key !== 'undefined' ? i.key : i.id;
                if (key === itemKey) {
                    return i;
                } else {
                    return null;
                }
            });
            this.props.onSelect(item);
        }
    };

    /**
     *  Renderovánaí samotné komponenty přepínacích záložek
     **/
    render() {
        let tabs = this.props.items.map((item, i) => {
            let closeTitle = i18n('tabs.action.closeTab');
            let closeAction;
            if (this.props.closable) {
                closeAction = (
                    <NoFocusButton title={closeTitle} onClick={this.handleTabClose.bind(this, item)}>
                        <Icon glyph="fa-times"/>
                    </NoFocusButton>
                );
            }

            // popisek ikony zavírající záložku
            let key = typeof item.key !== 'undefined' ? item.key : item.id;
            return (
                <Nav.Item key={key} as="li">
                    <Nav.Link tabIndex={-1} ref={'tab' + i} eventKey={key}>
                        <span title={item.title || item.name}>{item.title || item.name}</span>

                        <small>{item.desc}</small>
                        {closeAction}
                    </Nav.Link>
                </Nav.Item>
            );
        });

        // vrácení html komponenty záložek
        let activeKey = null;
        if (this.props.activeItem) {
            activeKey =
                typeof this.props.activeItem.key !== 'undefined' ? this.props.activeItem.key : this.props.activeItem.id;
        }
        return (
            <Nav
                as="ul"
                className="tabs-tabs-container"
                ref="tabs"
                variant="tabs"
                onSelect={this.handleTabSelect}
                activeKey={activeKey}
            >
                {tabs}
            </Nav>
        );
    }
};
