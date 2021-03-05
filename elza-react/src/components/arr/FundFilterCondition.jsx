/**
 * Nastavení podmínky na filtr - filtrování pol podmíky.
 */

import './FundFilterSettings.scss';

import React from 'react';
import {AbstractReactComponent, FormInput} from 'components/shared';
import {getMapFromList} from 'stores/app/utils';

class FundFilterCondition extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderValues', 'handleCodeChange', 'handleChangeValue');

        this.state = {
            values: props.values,
            errors: [],
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {}

    componentDidMount() {}

    handleCodeChange(e) {
        const {values, onChange} = this.props;

        onChange(e.target.value, values);
    }

    handleChangeValue(index, value) {
        const {values, items, selectedCode, onChange, validateField, normalizeField} = this.props;
        const {errors} = this.state;
        const itemsCodeMap = getMapFromList(items, 'code');
        const selectedItem = itemsCodeMap[selectedCode];

        var newValues = [...values];

        // Normalizace
        var updatedValue = normalizeField ? normalizeField(selectedCode, selectedItem.values, value, index) : value;
        newValues[index] = updatedValue;

        // Validace
        var hasErrors = false;
        var newErrors = [...errors];
        const error = validateField(selectedCode, selectedItem.values, updatedValue, index);
        if (error instanceof Promise) {
            // promise pro validaci - asi serverová validace
            hasErrors = true; // nevíme, zda projde validace, raději nastavíme, že je chyba - aby nešel formulář uložit

            // Zavolání asynchronnní validace
            error
                .then(error => {
                    var {values, selectedCode} = this.props;
                    var {errors} = this.state;
                    var newErrors = [...errors];
                    newErrors[index] = error;

                    this.setState({errors: newErrors});

                    // Existují nějaké chyby?
                    var hasErrors = false;
                    newErrors.forEach(err => {
                        if (err) {
                            hasErrors = true;
                        }
                    });

                    // On change
                    onChange(selectedCode, values, hasErrors);
                })
                .catch(() => {});
        } else {
            // vlastní validační hláška
            newErrors[index] = error;
        }

        this.setState({errors: newErrors});

        // Existují nějaké chyby?
        newErrors.forEach(err => {
            if (err) {
                hasErrors = true;
            }
        });

        // On change
        onChange(selectedCode, newValues, hasErrors);
    }

    renderValues() {
        const {values, items, selectedCode} = this.props;
        const {errors} = this.state;
        const itemsCodeMap = getMapFromList(items, 'code');
        const selectedItem = itemsCodeMap[selectedCode];

        var fields = [];
        for (var a = 0; a < selectedItem.values; a++) {
            fields.push({
                value: values[a],
                error: errors[a],
                onChange: this.handleChangeValue.bind(this, a),
            });
        }

        return this.props.renderFields(fields);
    }

    render() {
        const {items, selectedCode, className, label} = this.props;
        const lbl = label ? <h4>{label}</h4> : null;

        var cls = className ? className + ' filter-condition-container' : 'filter-condition-container';

        return (
            <div className={cls}>
                {lbl}
                <div className="inputs-container">
                    <FormInput as="select" onChange={this.handleCodeChange} value={selectedCode}>
                        {items.map(i => {
                            return (
                                <option key={i.code} value={i.code}>
                                    {i.name}
                                </option>
                            );
                        })}
                    </FormInput>
                </div>
                <div className="vlaues-container">{this.renderValues()}</div>
            </div>
        );
    }
}

export default FundFilterCondition;
