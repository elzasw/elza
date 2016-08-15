import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, Icon, i18n, Loading} from 'components/index.jsx';
import {SplitButton, MenuItem} from 'react-bootstrap';
import {WebApi} from 'actions/index.jsx';

const INIT_STATE = {
    items: undefined,
    loading: false,
    open: false
};

const AddNodeDropdown = class AddNodeDropdown extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('focusFirstMenuItem', 'handleToggle');

        this.state = INIT_STATE;
    }

    /**
     * Renderuje dropdown pro přidání JP se scénáři
     */
    render() {
        const menuItems = [];
        const {items, loading, open} = this.state;
        if (items) {
            items.map((item, index) => {
                if (index === 0) {
                    menuItems.push(<MenuItem key={'add-item-' + index} ref='firstMenuItem' eventKey={item.name}>{item.name}</MenuItem>)
                } else {
                    menuItems.push(<MenuItem key={'add-item-' + index} eventKey={item.name}>{item.name}</MenuItem>)
                }
            });

            if (items.length === 0) {
                menuItems.push(<MenuItem key={'add-item-' + 0} ref='firstMenuItem' eventKey={null}>{i18n('subNodeForm.add.noScenario')}</MenuItem>)
            } else {
                menuItems.push(<MenuItem key={'add-item-' + items.length} eventKey={null}>{i18n('subNodeForm.add.noScenario')}</MenuItem>)
            }
        }

        const {key, action, glyph, title} = this.props;
        return (
            <SplitButton bsStyle="default"
                         tabIndex={-1}
                         id={`dropdown-${key}`}
                         key={key}
                         onSelect={() => {action()}}
                         onClick={(isOpen) => {this.handleToggle(isOpen, false)}} // Klik na tlačítko
                         onToggle={(isOpen) => {this.handleToggle(isOpen, true)}} // Klik na dropdown
                         open={open}
                         title={<span>{glyph && <Icon glyph={glyph} />} {title}</span>}>
                {loading && <Loading />}
                {menuItems}
            </SplitButton>
        )
    }

    focusFirstMenuItem() {
        // TODO - není dobré řešení, ale v tuto chvíli mě jiné nenapadá
        ReactDOM.findDOMNode(this.refs.firstMenuItem).children[0].focus()
    }

    /**
     * Načte scénáře a vrátí je pro zobrazení v DropDown
     */
    handleToggle(isOpen, isDropdown) {
        if (isOpen && !this.state.loading) {
            this.setState({
                ...this.state,
                loading: true
            });
            const {node, version, direction} = this.props;
            WebApi.getNodeAddScenarios(node, version, direction).then((result) => {
                if (isDropdown || result.length > 1) {
                    this.setState({
                        ...this.state,
                        items: result,
                        open: true,
                        loading: false
                    }, ()=>{this.focusFirstMenuItem()});
                } else if (result.length === 1) {
                    this.props.action(undefined, result[0].name);
                    this.setState(INIT_STATE);
                } else {
                    this.props.action();
                    this.setState(INIT_STATE);
                }
            });
        } else {
            this.setState({
                ...this.state,
                items: undefined,
                open: false
            });
        }
    }
};

AddNodeDropdown.propTypes = {
    title: React.PropTypes.any.isRequired,
    glyph: React.PropTypes.string,
    version: React.PropTypes.number.isRequired,
    node: React.PropTypes.object.isRequired,
    direction: React.PropTypes.oneOf(['BEFORE','AFTER','CHILD']).isRequired,
    action: React.PropTypes.func.isRequired
};

module.exports = AddNodeDropdown;



