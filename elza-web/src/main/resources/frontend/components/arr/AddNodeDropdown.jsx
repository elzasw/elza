import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, Icon, i18n} from 'components/index.jsx';
import Button from '../../node_modules/react-bootstrap/lib/Button';
import SplitButton from '../../node_modules/react-bootstrap/lib/SplitButton';
import MenuItem from '../../node_modules/react-bootstrap/lib/MenuItem';
import Loading from '../shared/loading/Loading.jsx'
import {WebApi} from 'actions/index.jsx';

var initState = {
    items: undefined,
    loading: false,
    open: false
};

var AddNodeDropdown = class AddNodeDropdown extends AbstractReactComponent {


    constructor(props) {
        super(props);

        this.bindMethods('focusFirstMenuItem')

        this.state = initState;
    }

    /**
     * Renderuje dropdown pro přidání JP se scénáři
     */
    render() {
        var items = []
        if (this.state.items) {
            this.state.items.map((item, index) => {
                if (index === 0) {
                    items.push(<MenuItem ref='firstMenuItem' eventKey={item.name}>{item.name}</MenuItem>)
                } else {
                    items.push(<MenuItem eventKey={item.name}>{item.name}</MenuItem>)
                }
            })
            if (items.length === 0) {
                items.push(<MenuItem ref='firstMenuItem' eventKey={null}>Bez scénáře</MenuItem>)
            } else {
                items.push(<MenuItem eventKey={null}>Bez scénáře</MenuItem>)
            }
        }

        return (
            <SplitButton bsStyle="default"
                         tabIndex={-1}
                         id={`dropdown-${this.props.key}`}
                         key={this.props.key}
                         onSelect={this.props.action}
                         onClick={(isOpen) => {this.handleToggle(isOpen, false)}} // Klik na tlačítko
                         onToggle={(isOpen) => {this.handleToggle(isOpen, true)}} // Klik na dropdown
                         open={this.state.open}
                         title={<span>{this.props.glyph && <Icon glyph={this.props.glyph} />} {this.props.title}</span>}>
                {this.state.loading && <Loading />}
                {items}
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
        // TODO Zkontrolovat načítání
        if (isOpen && !this.state.loading) {
            this.setState({
                ...this.state,
                loading: true
            });
            WebApi.getNodeAddScenarios(this.props.node, this.props.version, this.props.direction).then((result) => {
                if (isDropdown || result.length > 1) {
                    this.setState({
                        ...this.state,
                        items: result,
                        open: true,
                        loading: false
                    }, ()=>{this.focusFirstMenuItem()});
                } else if (result.length === 1) {
                    this.props.action(undefined, result[0].name);
                    this.setState(initState);

                } else {
                    this.props.action();
                    this.setState(initState);
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
    title: React.PropTypes.string.isRequired,
    glyph: React.PropTypes.string.isRequired,
    version: React.PropTypes.number.isRequired,
    node: React.PropTypes.object.isRequired,
    direction: React.PropTypes.string.isRequired
};

module.exports = AddNodeDropdown;



