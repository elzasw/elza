// --
import PropTypes from 'prop-types';

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {indexById} from 'stores/app/utils';
import Tags from 'components/form/Tags';

/**
 * Formulář pro vybrání několika položek pomocí tag input.
 */
class SelectItemsForm extends AbstractReactComponent {
    static propTypes = {
        renderItem: PropTypes.func.isRequired, // render položky v tag input, přepis: (item, isHighlighted = false, isSelected = false) => {}
        fieldComponent: PropTypes.func.isRequired, // reference na komponentu fieldu pro input - dohledání položky, např. GroupField
        fieldComponentProps: PropTypes.object, // props pro fieldComponent
        onSubmitForm: PropTypes.func.isRequired, // callback se seznamem vybraných položek - pro přídání, předpis: (items : array) => {}
    };

    constructor(props) {
        super(props);

        this.state = {
            items: [],
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {}

    handleRemoveItem = (item, itemIndex) => {
        const {items} = this.state;

        const index = indexById(items, item.id);
        if (index !== null) {
            this.setState({
                items: [...items.slice(0, index), ...items.slice(index + 1)],
            });
        }
    };

    handleChange = item => {
        console.log('select items form', item);
        if (item) {
            const {items} = this.state;

            const index = indexById(items, item.id);
            if (index === null) {
                this.setState({
                    items: [...items, item],
                });
            }
        }
    };

    render() {
        const {fieldComponent, fieldComponentProps, renderItem, onSubmitForm, onClose} = this.props;
        const {items} = this.state;

        const itemField = React.createElement(fieldComponent, {
            tags: true,
            ...fieldComponentProps,
            onChange: this.handleChange,
        });

        return (
            <div>
                <Modal.Body>
                    <div>
                        {itemField}
                        <Tags items={items} renderItem={renderItem} onRemove={this.handleRemoveItem} />
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        variant={'outline-secondary'}
                        onClick={() => {
                            onSubmitForm(items);
                        }}
                    >
                        {i18n('global.action.add')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
}

export default SelectItemsForm;
