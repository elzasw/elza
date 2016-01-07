import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {ResizeStore} from 'stores';

var AccordionComponent = require('./AccordionComponent');

var Accordion = class AccordionWrapper extends AbstractReactComponent {

    constructor(props) {
        super(props);

        this.bindMethods('renderItem', 'setHeights');

        ResizeStore.listen(status => {
            this.setHeights();
        });

        this.state = {};
    }

    componentDidMount() {
        this.setHeights();
    }

    setHeights() {
        if (this.refs.contentContainer) {
            var itemCloseHeight = 50;
            var itemOpenHeight = this.refs.contentContainer.clientHeight - itemCloseHeight * 4;

            this.setState({itemCloseHeight:itemCloseHeight, itemOpenHeight: itemOpenHeight});
        }
    }

    renderItem(item) {

        var selectedNodeId = this.props.selectedId;
        var open = item.id == selectedNodeId;
        var result;


        if (open) {
            result = (
                    <div style={{border: '1px solid black', height: this.state.itemOpenHeight}} key={item.id} >
                        <div style={{height: this.state.itemCloseHeight}} onClick={this.props.closeItem.bind(this, item)}>{item.id}</div>
                        <div style={{height: this.state.itemOpenHeight-this.state.itemCloseHeight, 'overflow-y': 'auto'}}>
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            Tadaaaa<br />
                            {false && <NodeForm levelExt={this.props.nodeForm.levelExt}/>}
                        </div>
                    </div>

            )
        } else {
            result = <div style={{border: '1px solid black', height: this.state.itemCloseHeight}} key={item.id} onClick={this.props.openItem.bind(this, item)}>{item.id}</div>
        }

        return (
                result
        )
    }

    render() {

        var scrollTo;

        if (this.refs.contentContainer) {

            var items = this.props.items;
            var selectedId = this.props.selectedId;

            if (selectedId != null) {
                var index = items.map(item=>{return item.id}).indexOf(selectedId);
                var count = items.length;

                var x = this.state.itemOpenHeight / this.state.itemCloseHeight * 2;

                var offset = ((this.refs.contentContainer.clientHeight - this.state.itemOpenHeight) / 2);

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
                <div className="content" ref="contentContainer">
                    {this.refs.contentContainer && <AccordionComponent container={this.refs.contentContainer} scrollTo={scrollTo} renderItem={this.renderItem} selectedId={this.props.selectedId} items={this.props.items} itemCloseHeight={this.state.itemCloseHeight} itemOpenHeight={this.state.itemOpenHeight} />}
                </div>
        )
    }
}

module.exports = Accordion;


