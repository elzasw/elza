/**
 * Dialog pro přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 26.8.2016
 */
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {Button} from '../../ui';
import {Col, Form, FormControl, FormGroup, FormLabel, Modal, Row} from 'react-bootstrap';
import {WebApi} from 'actions/index';
import {AbstractReactComponent, Autocomplete, FormInput, HorizontalLoader, i18n} from 'components/shared';
import {getOneSettings, isFundRootId} from 'components/arr/ArrUtils';
import {getSetFromIdsList, indexById} from 'stores/app/utils';
import './AddNodeForm.scss';

import FundTreeCopy from '../FundTreeCopy';
import FundField from '../../admin/FundField';
import {FUND_TREE_AREA_COPY} from '../../../actions/constants/ActionTypes';
import {nodeFormActions} from '../../../actions/arr/subNodeForm';
import {JAVA_ATTR_CLASS} from '../../../constants';
import RefTemplateField from '../RefTemplateField';

const TEMPLATE_SCENARIOS = 'TEMPLATE_SCENARIOS';

class AddNodeForm extends AbstractReactComponent {
    static propTypes = {
        node: PropTypes.object.isRequired, // node pro který se akce volá
        parentNode: PropTypes.object.isRequired, // nadřený node pro vstupní node
        initDirection: PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND']),
        nodeSettings: PropTypes.object.isRequired,
        onSubmit: PropTypes.func.isRequired,
        allowedDirections: PropTypes.arrayOf(PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND'])),
    };

    state = {
        // initial states
        count: 1,
        scenarios: undefined,
        template: '',
        loading: false,
        selectedDirection: this.props.initDirection,
        selectedScenario: undefined,
        allowedDirections: ['BEFORE', 'AFTER', 'CHILD', 'ATEND'],
        selectedType: 'NEW',
        selectedSourceAS: 'FILE',
        scopeList: [],
        value: '',
        ignoreRootNodes: false,
        submitting: false,
        valid: true,
        //AKA refTemplateId
        templateId: undefined,
        refTemplatesLoad: {
            loaded: false,
            data: []
        }
    };

    validate = (props, state) => {
        const {selectedType, selectedSourceAS, selectedScope, importXml} = state;
        let errors = [];
        if (selectedType != 'NEW' && selectedSourceAS === 'FILE') {
            if (!selectedScope || selectedScope.id < 0) {
                errors.push('no scope selected');
            }
            if (!importXml) {
                errors.push('no file selected');
            }
        }
        if (selectedType != 'NEW' && selectedSourceAS === 'OTHER') {
            const selectedIds = props.fundTreeCopy ? props.fundTreeCopy.selectedIds : {};
            if (Object.keys(selectedIds).length === 0) {
                errors.push('no selected id');
            }
        }
        return errors;
    };
    /**
     * Připravuje hodnoty proměnných rodič, směr pro potřeby API serveru
     * - Přidání JP na konec se mění na přidání dítěte rodiči
     * @param {String} inDirection směr, kterým se má vytvořit nová JP
     * @param {Object} inNode uzel pro který je volána akce
     * @param {Object} inParentNode nadřazený uzel k inNode
     */
    formatDataForServer = (inDirection, inNode, inParentNode) => {
        let direction, node, parentNode;

        switch (inDirection) {
            case 'ATEND':
                direction = 'CHILD';
                node = inParentNode;
                parentNode = inParentNode;
                break;
            case 'CHILD':
                direction = inDirection;
                node = inNode;
                parentNode = inNode;
                break;
            case 'BEFORE':
            case 'AFTER':
                direction = inDirection;
                node = inNode;
                parentNode = inParentNode;
                break;
            default:
                break;
        }

        return {
            direction: direction,
            activeNode: node,
            parentNode: parentNode,
        };
    };

    /**
     * Načte scénáře a vrátí je pro zobrazení, v průběhu načístání nastaví ve state 'loading'
     * resp. uloží je do state 'scenarios'
     */
    getDirectionScenarios = newDirection => {
        if (newDirection === '') {
            // direction is not selected
            this.setState({
                scenarios: undefined,
            });
            return;
        }
        // direction is selected
        this.setState({
            loading: true,
        });
        const {node, parentNode, versionId} = this.props;
        // nastavi odpovidajiciho rodice a direction pro dotaz
        var dataServ = this.formatDataForServer(newDirection, node, parentNode);
        // ajax dotaz na scenare
        WebApi.getNodeAddScenarios(dataServ.activeNode, versionId, dataServ.direction).then(
            result => {
                // resolved
                // select first scenario
                var selScn = undefined;
                if (result.length > 0) selScn = result[0].name;
                this.setState({
                    scenarios: result,
                    loading: false,
                    selectedScenario: selScn,
                });
            },
            reason => {
                // rejected
                this.setState({
                    scenarios: undefined,
                    loading: false,
                    selectedScenario: undefined,
                });
            },
        );
    };

    handleDirectionChange = e => {
        var dr = e.target.value;
        this.setState({selectedDirection: dr});
        this.getDirectionScenarios(dr);
    };

    handleScenarioChange = e => {
        if (e.target.value === TEMPLATE_SCENARIOS) {
            const {arrRegion, userDetail} = this.props;

            const settings = userDetail.settings;
            const fund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
            let lastUseTemplateName = null;
            if (fund) {
                lastUseTemplateName = this.getLastUseTemplateName(settings, fund);
            }
            this.setState({selectedScenario: e.target.value, template: lastUseTemplateName});
        } else {
            this.setState({selectedScenario: e.target.value});
        }
    };

    getLastUseTemplateName = (settings, fund) => {
        const fundTemplates = getOneSettings(settings, 'FUND_TEMPLATES', 'FUND', fund.id);
        const templates = fundTemplates.value ? JSON.parse(fundTemplates.value).map(template => template.name) : [];
        return templates.indexOf(fund.lastUseTemplateName) >= 0 ? fund.lastUseTemplateName : null;
    };

    handleTemplateChange = e => {
        this.setState({template: e.target.value});
    };

    handleCountChange = (e) => {
        if(Number.isInteger(parseInt(e.target.value))){
            this.setState({count: e.target.value})
        }
    };

    /**
     * Vrátí prvky popisu ke zkopírování na základě proměnné props.nodeSettings
     */
    getDescItemTypeCopyIds = () => {
        const nodeSettings = this.props.nodeSettings;
        const node = this.props.activeFund;
        const nodeId = this.props.parentNode.id;

        let itemsToCopy = null;
        if (nodeSettings != undefined) {
            const nodeIndex = indexById(nodeSettings.nodes, nodeId);
            if (nodeIndex != null) {
                let nodeSetting = nodeSettings.nodes[nodeIndex];
                if (nodeSetting.copyAll) {
                    // najít aktivní node
                    let activeNode = this.props.activeFund.nodes.nodes[this.props.activeFund.nodes.activeIndex];

                    // všechny ID descItemTypes z aktivního node
                    itemsToCopy = [];
                    for (let a = 0; a < activeNode.subNodeForm.formData.descItemGroups.length; a++) {
                        // pro všecny DescItemGroups
                        let descItemGroup = activeNode.subNodeForm.formData.descItemGroups[a];
                        for (let i = 0; i < descItemGroup.descItemTypes.length; i++) {
                            // pro všechny položky na formuláři
                            let descItemType = descItemGroup.descItemTypes[i];
                            itemsToCopy = [...itemsToCopy, descItemType.id];
                        }
                    }
                } else {
                    itemsToCopy = nodeSetting.descItemTypeCopyIds; // jen vyjmenované ID
                }
            }
        }
        return itemsToCopy;
    };

    notEmpty = value => {
        return value != null && value !== '';
    };

    /**
     * Zprostředkuje přidání nové JP, dle aktuálně vyplněných dat ve formuláři
     * resp. dat uložených ve state 'selectedDirection', 'selectedScenario'
     */
    handleFormSubmit = e => {
        e.preventDefault();
        const {
            onSubmit,
            node,
            parentNode,
            versionId,
            activeFund,
            userDetail,
            initDirection,
            globalFundTree: {fundTreeCopy},
        } = this.props;
        const {selectedDirection, selectedScenario, template, count} = this.state;

        // nastavi odpovidajiciho rodice a direction pro dotaz
        const dataServ = this.formatDataForServer(selectedDirection, node, parentNode);
        if (this.state.selectedType === 'NEW') {
            // Data pro poslání do on submit - obsahují všechny informace pro založení
            const submitData = {
                indexNode: dataServ.activeNode,
                parentNode: dataServ.parentNode,
                versionId: versionId,
                direction: dataServ.direction,
                descItemCopyTypes: this.getDescItemTypeCopyIds(),
                scenarioName: selectedScenario === TEMPLATE_SCENARIOS ? null : selectedScenario,
                count,
            };

            const emptyItemTypeIds = []; // seznam identifikátorů prázdných typů atributu, které se mají po založení přidat na formulář (použití pro šablony)
            if (selectedScenario === TEMPLATE_SCENARIOS) {
                if (activeFund) {
                    let settings = userDetail.settings;
                    const fundTemplates = getOneSettings(settings, 'FUND_TEMPLATES', 'FUND', activeFund.id);

                    const value = JSON.parse(fundTemplates.value);
                    const index = indexById(value, template, 'name');

                    if (index == null) {
                        console.error('Nebyla nalezena šablona s názvem: ' + template);
                    } else {
                        const template = value[index];
                        if (template.formData != null) {
                            const formData = template.formData;
                            const createItems = [];
                            Object.keys(formData).forEach(itemTypeId => {
                                const items = formData[itemTypeId];
                                items.forEach(item => {
                                    if (
                                        this.notEmpty(item.value) ||
                                        (item[JAVA_ATTR_CLASS] === '.ArrItemEnumVO' &&
                                            this.notEmpty(item.descItemSpecId))
                                    ) {
                                        const newItem = {
                                            ...item,
                                            itemTypeId: itemTypeId,
                                        };
                                        createItems.push(newItem);
                                    } else {
                                        emptyItemTypeIds.push(parseInt(itemTypeId));
                                    }
                                });
                            });
                            if (createItems.length > 0) {
                                submitData.createItems = createItems;
                            }
                        }
                        this.props.dispatch(
                            nodeFormActions.fundSubNodeFormTemplateUseOnly(activeFund.versionId, template),
                        );
                    }
                }
            }

            onSubmit(submitData, 'NEW', null, emptyItemTypeIds);
        } else if (this.state.selectedSourceAS === 'FILE') {
            const submitData = {
                xmlFile: this.state.importXml,
                scopeId: this.state.selectedScope.id,
                ignoreRootNodes: this.state.ignoreRootNodes,
            };
            //Upravena rozhodovaci logika pro umisteni uzlu
            let importPositionParams = {
                fundVersionId: versionId,
            };
            if (selectedDirection === 'CHILD') {
                importPositionParams.parentNode = node;
                importPositionParams.direction = 'AFTER';
            } else if (selectedDirection === 'ATEND') {
                importPositionParams.parentNode = parentNode;
                importPositionParams.direction = 'AFTER';
            } else {
                importPositionParams.targetNode = node;
                importPositionParams.parentNode = parentNode;
                importPositionParams.direction = selectedDirection;
            }
            submitData.importPositionParams = new Blob([JSON.stringify(importPositionParams)], {
                type: 'application/json',
            });

            onSubmit(submitData, 'FILE');
        } else if (this.state.selectedSourceAS === 'OTHER') {
            const newNode = {
                id: node.id,
                version: node.version,
            };

            const sourceNodes = [];

            for (let nodeItem in fundTreeCopy.selectedIds) {
                const nodeId = parseInt(nodeItem);
                let node;
                fundTreeCopy.nodes.forEach(i => {
                    if (i.id === nodeId) {
                        node = i;
                    }
                });
                if (node) {
                    sourceNodes.push({
                        id: node.id,
                        version: node.version,
                    });
                }
            }

            const submitData = {
                targetFundVersionId: versionId,
                targetStaticNode: newNode,
                targetStaticNodeParent: getParentNode(parentNode),
                selectedDirection,
                sourceFundVersionId: this.props.globalFundTree.versionId,
                sourceNodes,
                ignoreRootNodes: this.state.ignoreRootNodes,
                templateId: this.state.templateId,
            };

            this.setState({submitting: true});
            onSubmit(submitData, 'OTHER', () => {
                this.setState({submitting: false});
            });
        }
    };

    componentDidMount() {
        const {initDirection} = this.props;
        this.getDirectionScenarios(initDirection);
        this.fetchScopeList();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const errors = this.validate(nextProps, this.state);
        if (errors.length === 0) {
            this.setState({valid: true});
        }
    }

    fetchScopeList = () => {
        WebApi.getScopes(this.props.versionId).then(data => {
            const selectedScope = data[0] || undefined;
            this.setState({
                scopeList: data,
                selectedScope,
            });
        });
    };

    changeNodeSource = (type, source) => {
        return e => {
            const nextState = {...this.state};
            if (type) {
                nextState.selectedType = type;
            }
            if (source) {
                nextState.selectedSourceAS = source;
            }

            const errors = this.validate(this.props, nextState);

            this.setState({
                ...nextState,
                valid: errors.length === 0,
            });
        };
    };

    render() {
        const {
            allowedDirections,
            onClose,
            node,
            parentNode,
            versionId,
            initDirection,
            arrRegion,
            userDetail,
        } = this.props;
        const {scenarios, loading, submitting, valid} = this.state;
        const notRoot = !isFundRootId(parentNode.id);

        // Položky v select na směr
        const allowedDirectionsMap = getSetFromIdsList(allowedDirections);
        const directions = {
            BEFORE: 'before',
            AFTER: 'after',
            CHILD: 'child',
            ATEND: 'atEnd',
        };
        const options = [];
        for (let d in directions) {
            if (allowedDirectionsMap[d]) {
                if (isFundRootId(parentNode.id) && d !== 'CHILD') {
                    continue;
                }
                options.push(
                    <option value={d} key={d}>
                        {i18n(`arr.fund.addNode.${directions[d]}`)}
                    </option>,
                );
            }
        }

        return (
            <Form>
                <Modal.Body>
                    <FormGroup>
                        <Row>
                            <Col xs={2}>
                                <Form.Check
                                    id={'selectType-NEW'}
                                    inline
                                    type={'radio'}
                                    name="selectType"
                                    checked={this.state.selectedType === 'NEW'}
                                    onChange={this.changeNodeSource('NEW')}
                                    label={i18n('arr.fund.addNode.type.new')}
                                />
                            </Col>
                            <Col xs={3}>
                                <Form.Check
                                    id={'selectType-EXISTING'}
                                    inline
                                    type={'radio'}
                                    name="selectType"
                                    checked={this.state.selectedType === 'EXISTING'}
                                    onChange={this.changeNodeSource('EXISTING')}
                                    label={i18n('arr.fund.addNode.type.existing')}
                                />
                            </Col>
                        </Row>
                        <Row>
                            <Col xs={12}>
                                <FormInput
                                    ref="selsel"
                                    as="select"
                                    disabled={loading || submitting}
                                    label={i18n('arr.fund.addNode.direction')}
                                    defaultValue={initDirection}
                                    onChange={this.handleDirectionChange}
                                >
                                    {options}
                                </FormInput>
                            </Col>
                        </Row>
                    </FormGroup>
                    {this.state.selectedType === 'NEW' ? this.renderCreateNew() : this.renderCreateExisting()}
                </Modal.Body>
                <Modal.Footer>
                    {this.state.selectedType === 'EXISTING' && (
                        <Form.Check
                            id={'ignore-root-nodes'}
                            disabled={submitting}
                            type={'checkbox'}
                            inline
                            checked={this.state.ignoreRootNodes}
                            onChange={() => {
                                this.setState(prevState => {
                                    return {
                                        ignoreRootNodes: !prevState.ignoreRootNodes,
                                    };
                                });
                            }}
                            label={i18n('arr.fund.addNode.ignoreRootNodes')}
                            className={'mr-auto'}
                        />
                    )}
                    <Button
                        variant="outline-secondary"
                        disabled={submitting || !valid}
                        type="submit"
                        onClick={this.handleFormSubmit}
                    >
                        {i18n('global.action.store')}
                    </Button>
                    <Button disabled={submitting} variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }

    renderCreateNew() {
        const {arrRegion, userDetail} = this.props;
        const {scenarios, loading, submitting, selectedScenario, template} = this.state;

        let templates = [];
        let scnRadios = [];
        let lastUseTemplateName = null;
        if (!loading) {
            let i = 0;
            if (scenarios) {
                for (i; i < scenarios.length; i++) {
                    scnRadios.push(
                        <Form.Check
                            id={'scns-' + i}
                            type={'radio'}
                            key={'scns-' + i}
                            defaultChecked={i === 0}
                            autoFocus={i === 0}
                            name="scns"
                            onChange={this.handleScenarioChange}
                            value={scenarios[i].name}
                            checked={selectedScenario === scenarios[i].name}
                            label={scenarios[i].name}
                        />,
                    );
                }
            }

            let strictMode = false;
            const fund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

            const settings = userDetail.settings;

            if (fund) {
                const fundTemplates = getOneSettings(settings, 'FUND_TEMPLATES', 'FUND', fund.id);
                templates = fundTemplates.value ? JSON.parse(fundTemplates.value).map(template => template.name) : [];

                strictMode = fund.activeVersion.strictMode;

                let userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
                if (userStrictMode && userStrictMode.value !== null) {
                    strictMode = userStrictMode.value === 'true';
                }
            }

            if (!strictMode || i === 0) {
                scnRadios.push(
                    <Form.Check
                        type={'radio'}
                        key={'scns-' + i}
                        defaultChecked={i === 0}
                        autoFocus={i === 0}
                        name="scns"
                        onChange={this.handleScenarioChange}
                        value={''}
                        checked={selectedScenario === ''}
                        label={i18n('subNodeForm.add.noScenario')}
                    />,
                );
                i++;
            }

            scnRadios.push(
                <Form.Check
                    id={'snss-template'}
                    type={'radio'}
                    key={'tmpl'}
                    defaultChecked={i === 0}
                    autoFocus={i === 0}
                    name="scns"
                    onChange={this.handleScenarioChange}
                    value={TEMPLATE_SCENARIOS}
                    checked={selectedScenario === TEMPLATE_SCENARIOS}
                    label={i18n('subNodeForm.add.fromTemplate')}
                />,
            );
        } else {
            scnRadios.push(<div>{i18n('arr.fund.addNode.noDirection')}</div>);
        }

        let defaultValueTemplate = '';
        if (lastUseTemplateName) {
            defaultValueTemplate = lastUseTemplateName;
        }
        if (template) {
            defaultValueTemplate = template;
        }

        return (
            <div>
                <FormGroup>
                    <FormLabel>{i18n('arr.fund.addNode.scenario')}</FormLabel>
                    {loading ? (
                        <HorizontalLoader />
                    ) : (
                        <div>
                            <FormGroup key="Scenarios">{scnRadios}</FormGroup>
                            {selectedScenario === TEMPLATE_SCENARIOS && (
                                <FormInput
                                    ref="select"
                                    key={'tmpl-select'}
                                    as="select"
                                    name={'template'}
                                    disabled={loading || submitting}
                                    label={''}
                                    onChange={this.handleTemplateChange}
                                    defaultValue={defaultValueTemplate}
                                >
                                    <option value={''} key="no-select">
                                        {i18n('global.action.select')}
                                    </option>
                                    {templates.map(tmp => (
                                        <option value={tmp} key={tmp}>
                                            {tmp}
                                        </option>
                                    ))}
                                </FormInput>
                            )}
                        </div>
                    )}
                    <FormInput
                        ref="count"
                        disabled={loading || submitting}
                        label={i18n('arr.fund.addNode.count')}
                        type="number"
                        value={this.state.count}
                        onChange={this.handleCountChange}
                    />
                </FormGroup>
            </div>
        );
    }

    renderCreateExisting() {
        const {submitting} = this.state;
        return [
            <FormGroup>
                <Row>
                    <Col>
                        <Form.Check
                            type={'radio'}
                            disabled={submitting}
                            inline
                            name="selectSource"
                            checked={this.state.selectedSourceAS === 'FILE'}
                            onChange={this.changeNodeSource('EXISTING', 'FILE')}
                            label={i18n('arr.fund.addNode.type.existing.file')}
                            id={'select-source-file'}
                        />
                    </Col>
                    <Col>
                        <Form.Check
                            id={'select-source-other'}
                            type={'radio'}
                            disabled={submitting}
                            inline
                            name="selectSource"
                            checked={this.state.selectedSourceAS === 'OTHER'}
                            onChange={this.changeNodeSource('EXISTING', 'OTHER')}
                            label={i18n('arr.fund.addNode.type.existing.other')}
                        />
                    </Col>
                </Row>
            </FormGroup>,
            this.state.selectedSourceAS === 'OTHER' ? this.renderCreateFromOther() : this.renderCreateFromFile(),
        ];
    }

    setValidatedState = state => {
        const newState = {
            ...this.state,
            ...state,
        };
        const errors = this.validate(this.props, newState);
        const valid = errors.length === 0;
        this.setState({
            ...newState,
            valid,
        });
    };
    handleScopeChange = item => {
        this.setValidatedState({
            selectedScope: item,
        });
    };
    handleFileChange = e => {
        this.setValidatedState({
            importXml: e.target.files[0],
        });
    };

    renderCreateFromFile() {
        const {scopeList, submitting} = this.state;
        return [
            <FormGroup>
                <Autocomplete
                    disabled={submitting}
                    label={i18n('arr.fund.regScope')}
                    items={scopeList}
                    getItemId={item => (item ? item.id : null)}
                    getItemName={item => {
                        return item ? item.name : '';
                    }}
                    value={this.state.selectedScope}
                    onChange={this.handleScopeChange}
                />
            </FormGroup>,
            <FormGroup>
                <FormControl disabled={submitting} name="soubor" type="file" onChange={this.handleFileChange} />
            </FormGroup>,
        ];
    }

    renderCreateFromOther() {
        const {value, submitting, refTemplatesLoad} = this.state;
        const {fundTreeCopy, fund, activeFund, versionId} = this.props;

        return [
            <FormGroup>
                <FormLabel>{i18n('arr.fund.addNode.type.existing.archiveFile')}</FormLabel>
                <FundField
                    excludedId={versionId}
                    ref="fundField"
                    value={value}
                    onChange={item => {
                        this.props.dispatch({
                            type: 'SELECT_FUND_GLOBAL',
                            area: 'FUND_TREE_AREA_COPY',
                            fund: item,
                            versionId: item.versions[0].id,
                        });
                        this.setState(() => {
                            return {value: item.name};
                        });
                    }}
                />
            </FormGroup>,
            <div>
                {fund && <FormLabel>{i18n('arr.history.title.nodeChanges')}</FormLabel>}
                {fund && (
                    <FundTreeCopy
                        disabled={submitting}
                        className="fund-tree-container-fixed"
                        fund={fund}
                        cutLongLabels={true}
                        versionId={fund.versions[0].id || 0}
                        {...fundTreeCopy}
                    />
                )}
            </div>,
            <FormGroup>
                {refTemplatesLoad.loaded && refTemplatesLoad.data.length > 0 && <FormLabel>{i18n('arr.fund.addNode.refTemplate')}</FormLabel>}
                <RefTemplateField
                    fundId={activeFund.id}
                    useIdAsValue
                    hide={!refTemplatesLoad.loaded || refTemplatesLoad.data.length === 0}
                    onChange={templateId => this.setState({templateId})}
                    value={this.state.templateId}
                    onLoadTemplates={templates => {
                        console.log("onLoadTemplates", templates, activeFund.id);
                        this.setState({refTemplatesLoad: {loaded: true, data: templates}});
                    }}
                />
            </FormGroup>,
        ];
    }
}

function getParentNode(parentNode) {
    if (!parentNode || (parentNode && isNaN(parentNode.id))) {
        return null;
    }
    return {
        id: parentNode.id,
        version: parentNode.version,
    };
}

function mapStateToProps(state) {
    const {arrRegion, userDetail, registryDetail} = state;

    return {
        fund: arrRegion.globalFundTree.fund,
        activeFund: arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null,
        fundTreeCopy: arrRegion.globalFundTree.fundTreeCopy,
        globalFundTree: arrRegion.globalFundTree,
        nodeSettings: arrRegion.nodeSettings,
        arrRegion: arrRegion,
        userDetail: userDetail,
        registryDetail,
    };
}

export default connect(mapStateToProps)(AddNodeForm);
