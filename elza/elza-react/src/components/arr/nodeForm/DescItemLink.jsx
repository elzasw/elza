/** */
import {ConnectedProps} from 'components/arr/nodeForm/DescItemStructureRef';
import React, {ChangeEvent} from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {connect, DispatchProp} from 'react-redux';
import {normalizeString} from 'components/validate.jsx';
import {decorateValue, inputValue} from './DescItemUtils.jsx';
import DescItemLabel from './DescItemLabel.jsx';
import {IDescItemBaseProps} from 'components/arr/nodeForm/DescItemTypes';
import Icon from '../../shared/icon/Icon';
import {Button} from 'react-bootstrap';
import SelectSearchFundsForm from 'components/arr/SelectSearchFundsForm';
import {modalDialogHide, modalDialogShow} from '../../../actions/global/modalDialog';
import {WebApi} from '../../../actions';
import {CLS_CALCULABLE, ELZA_SCHEME_NODE} from '../../../constants';
import {routerNavigate} from '../../../actions/router';
import './DescItemLink.scss';
import RefTemplateField from '../RefTemplateField';
import {decorateAutocompleteValue} from './DescItemUtils';

const DescItemString_MAX_LENGTH = 1000;

/**
 * Input prvek pro desc item - typ STRING.
 */
class DescItemLink extends AbstractReactComponent {
    focusEl = null;
    focusEl2 = null;
    focus = () => {
        this.focusEl.focus();
    };

    handleChange = e => {
        const newValue = normalizeString(e.target.value, DescItemString_MAX_LENGTH);

        if (newValue !== this.props.descItem.value) {
            this.props.onChange({
                value: newValue,
                description: this.props.descItem.description,
                refTemplateId: this.props.descItem.refTemplateId,
            });
        }
    };
    handleDesc = e => {
        const newValue = normalizeString(e.target.value, DescItemString_MAX_LENGTH);

        if (newValue !== this.props.descItem.description) {
            this.props.onChange({
                description: newValue,
                value: this.props.descItem.value,
                refTemplateId: this.props.descItem.refTemplateId,
            });
        }
    };
    handleTemplate = newValue => {
        if (newValue !== this.props.descItem.refTemplateId) {
            this.props.onChange({
                description: this.props.descItem.description,
                value: this.props.descItem.value,
                refTemplateId: newValue,
            });
        }
    };

    search = () => {
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.title.search'),
                <SelectSearchFundsForm
                    onSubmit={({node, fund}) => {
                        // TODO new api
                        WebApi.getNode(fund.fundVersionId, node.id).then(data => {
                            this.props.onChange({
                                value: ELZA_SCHEME_NODE + '://' + data.uuid,
                                description:
                                    fund.id !== this.props.fundId ? data.fundName + ' ' + data.name : data.name,
                            });
                            this.props.onBlur();
                            this.props.dispatch(modalDialogHide());
                        });
                    }}
                />,
            ),
        );
    };

    handleNavigate = () => {
        const {descItem} = this.props;
        if (descItem.value.startsWith(ELZA_SCHEME_NODE)) {
            if (descItem.nodeId) {
                const uuid = descItem.value.replace(ELZA_SCHEME_NODE + '://', '');
                this.props.dispatch(routerNavigate(`/node/${uuid}`));
            }
        } else {
            window.open(descItem.value, '_blank');
        }
    };

    render() {
        const {descItem, locked, readMode, cal, fundId} = this.props;

        let value =
            cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : inputValue(descItem.value);

        const hasNodeLink = descItem.value && (descItem.value.startsWith(ELZA_SCHEME_NODE) || descItem.nodeId);

        if (readMode) {
            return (
                <DescItemLabel
                    onClick={hasNodeLink ? this.handleNavigate : null}
                    value={descItem.description || descItem.value}
                    cal={cal}
                    notIdentified={descItem.undefined}
                />
            );
        }

        let description = inputValue(descItem.description);

        let cls = [];
        if (cal) {
            cls.push(CLS_CALCULABLE);
        }

        return (
            <div className="flex flex-column w-100">
                <div className="desc-item-value desc-item-link">
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref={ref => (this.focusEl = ref)}
                        type="text"
                        disabled={locked || descItem.undefined}
                        value={descItem.undefined ? i18n('subNodeForm.descItemType.notIdentified') : value}
                        onChange={this.handleChange}
                        placeholder={i18n('subNodeForm.descItem.link.uri')}
                    />
                    <Button onClick={this.search}>
                        <Icon glyph={'fa-search'} />
                    </Button>
                    {hasNodeLink && (
                        <Button onClick={this.handleNavigate}>
                            <Icon glyph={'fa-external-link'} />
                        </Button>
                    )}
                </div>
                <div className="desc-item-value">
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref={ref => (this.focusEl2 = ref)}
                        type="text"
                        disabled={locked || descItem.undefined}
                        value={descItem.undefined ? i18n('subNodeForm.descItemType.notIdentified') : description}
                        onChange={this.handleDesc}
                        placeholder={i18n('subNodeForm.descItem.link.description')}
                    />
                </div>
                {descItem.nodeId && (
                    <RefTemplateField
                        {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        onChange={this.handleTemplate}
                        disabled={locked || descItem.undefined}
                        fundId={fundId}
                        useIdAsValue={true}
                        placeholder={i18n('subNodeForm.descItem.link.refTemplate')}
                        value={descItem.refTemplateId}
                    />
                )}
            </div>
        );
    }
}

export default connect(null, null, null, {forwardRef: true})(DescItemLink);
