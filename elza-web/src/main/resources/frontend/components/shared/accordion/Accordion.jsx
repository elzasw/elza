import React from 'react';
import ReactDOM from 'react-dom';
import AbstractReactComponent from "../../AbstractReactComponent";
import {store} from 'stores/index.jsx';
import AccordionComponent from './AccordionComponent';

import './Accordion.less'

class Accordion extends AbstractReactComponent {
    state = {};

    constructor(props) {
        super(props);

        store.listen(status => {
            this.setHeights();
        });
    }

    componentDidMount() {
        this.setHeights();
    }

    setHeights = () => {
        if (this.refs.contentContainer) {
            const itemCloseHeight = 42;
            const itemOpenHeight = this.refs.contentContainer.clientHeight - itemCloseHeight * 4;

            this.setState({itemCloseHeight, itemOpenHeight});
        }
    }

    renderItem = (item) => {
        const {selectedId} = this.props;

        if (item.id == selectedId) {
            return (
                <div className='accordion-item active' style={{height: this.state.itemOpenHeight}} key={item.id} >
                    <div className='accordion-header' style={{height: this.state.itemCloseHeight}} onClick={this.props.closeItem.bind(this, item)}>
                        <div className='accordion-header-content'>{this.props.renderItemHeader(item, true)}</div>
                    </div>
                    <div className='accordion-body' style={{height: this.state.itemOpenHeight-this.state.itemCloseHeight}}>
                        <div className='accordion-body-content'>
                            {this.props.renderItemContent(item)}
                        </div>
                    </div>
                </div>
            )
        } else {
            return (
                <div className='accordion-item' style={{height: this.state.itemCloseHeight}} key={item.id}>
                    <div className='accordion-header' style={{height: this.state.itemCloseHeight}} onClick={this.props.openItem.bind(this, item)}>
                        <div className='accordion-header-content'>{this.props.renderItemHeader(item, false)}</div>
                    </div>
                </div>
            )
        }
    };

    render() {

        let scrollTo;

        if (this.refs.contentContainer) {
            const {items, selectedId} = this.props;

            if (selectedId != null) {
                const index = items.map(item=>{return item.id}).indexOf(selectedId);

                // const x = this.state.itemOpenHeight / this.state.itemCloseHeight * 2;

                const offset = ((this.refs.contentContainer.clientHeight - this.state.itemOpenHeight) / 2);

                /*console.log(count, index, x);

                if (count - index < x) {
                    var k = count - index;
                    //offset = offset - 50;

                }*/

                scrollTo = this.state.itemCloseHeight * index - offset;
            }
        }

        console.log(offset);

        return (
            <div className="accordion" ref="contentContainer">
                {this.refs.contentContainer && <AccordionComponent container={this.refs.contentContainer} scrollTo={scrollTo} renderItem={this.renderItem} selectedId={this.props.selectedId} items={this.props.items} itemCloseHeight={this.state.itemCloseHeight} itemOpenHeight={this.state.itemOpenHeight} />}
            </div>
        )
    }
}

export default Accordion;


