import {_, ng, template, toasts} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {
    Campaign,
    COMBO_LABELS,
    Equipment,
    EquipmentOption,
    StructureGroup,
    StructureGroupImporter,
    TechnicalSpec,
    Utils
} from '../../model';

export const configurationController = ng.controller('configurationController',
    ['$scope', ($scope) => {
        $scope.pageSize = 20;
        $scope.nbItemsDisplay = $scope.pageSize;
        $scope.itemsFilter = 'name';
        $scope.filterOrder = false;
        $scope.COMBO_LABELS = COMBO_LABELS;
        $scope.display = {
            lightbox: {
                agent: false,
                supplier: false,
                contract: false,
                tag: false,
                equipment: false,
                campaign: false,
                structureGroup: false
            },
            input: {
                group: []
            }
        };

        $scope.sort = {
            agent: {
                type: 'name',
                reverse: false
            },
            supplier: {
                type: 'name',
                reverse: false
            },
            contract: {
                type: 'start_date',
                reverse: false
            },
            tag: {
                type: 'name',
                reverse: false
            },
            equipment: $scope.equipments.sort
        };

        $scope.search = {};

        $scope.filterEquipments = (type: string, reverse: boolean) => {
            $scope.nbItemsDisplay = $scope.pageSize;
            switch(type) {
                case 'name': $scope.itemsFilter = 'name'; break;
                case 'price': $scope.itemsFilter = 'price'; break;
                case 'supplier': $scope.itemsFilter = 'supplier_name'; break;
                case 'contract': $scope.itemsFilter = 'contract_name'; break;
                case 'status': $scope.itemsFilter = 'status'; break;
                default: $scope.itemsFilter = 'reference'; break;
            }
            $scope.filterOrder = reverse;
            $scope.sort.equipment.reverse = reverse;
            $scope.sort.equipment.type = type;
        };

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.addEquipmentFilter = (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                $scope.equipments.sort.filters = [...$scope.equipments.sort.filters, event.target.value];
                $scope.nbItemsDisplay = $scope.pageSize;
                $scope.equipments.sync(true, undefined, undefined, $scope.sort.equipment);
                event.target.value = '';
            }
        };

        $scope.dropEquipmentFilter = (filter: string) => {
            $scope.equipments.sort.filters = _.without($scope.equipments.sort.filters, filter);
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.sync(true, undefined, undefined, $scope.sort.equipment);
        };

        $scope.addTechnicalSpec = (equipment: Equipment) => {
            if(equipment.technical_specs == null){
                equipment.technical_specs=  [];
            }
            equipment.technical_specs.push(new TechnicalSpec());
            Utils.safeApply($scope);
        };

        $scope.dropTechnicalSpec = (equipment: Equipment, technicalSpec: TechnicalSpec) => {
            equipment.technical_specs = _.without(equipment.technical_specs, technicalSpec);
            Utils.safeApply($scope);
        };
        $scope.dropOption = (equipment: Equipment, index) => {
            equipment.deletedOptions === undefined ? equipment.deletedOptions = [] : null;
            equipment.options[index].id ? equipment.deletedOptions.push(equipment.options[index]) : null;
            equipment.options.splice(index, 1);
            Utils.safeApply($scope);
        };

        $scope.calculatePriceOption = (price, tax_id, amount) => {
            if (!price || !tax_id || !amount) return '';
            let tax_value = parseFloat(_.findWhere($scope.taxes.all, {id: tax_id}).value);
            if (tax_value !== undefined) {
                let priceFloat = parseFloat(price);
                let price_TTC = $scope.calculatePriceTTC(priceFloat);
                let Price_TTC_QTe = (price_TTC * parseFloat(amount));
                return (!isNaN(Price_TTC_QTe) && price_TTC !== '') ? Price_TTC_QTe.toFixed(2) : '';
            } else {
                return NaN;
            }
        };

        $scope.addOptionLigne = () => {
            let option = new EquipmentOption();
            $scope.equipment.options.push(option);
            Utils.safeApply($scope);
        };

        $scope.openCampaignForm = async (campaign: Campaign = new Campaign()) => {
            let id = campaign.id;
            id ? $scope.redirectTo('/campaigns/update') : $scope.redirectTo('/campaigns/create');
            $scope.campaign = new Campaign();
            Mix.extend($scope.campaign, campaign);
            await $scope.structureGroups.sync();
            id ? $scope.updateSelectedCampaign(id) : null;
            Utils.safeApply($scope);
        };

        $scope.updateSelectedCampaign = async (id) => {
            await $scope.campaign.sync(id, $scope.tags.all);
            $scope.structureGroups.all = $scope.structureGroups.all.map((group) => {
                let Cgroup = _.findWhere($scope.campaign.groups, {id: group.id});
                if (Cgroup !== undefined) {
                    group.selected = true;
                    group.tags = Cgroup.tags;
                }
                return group;
            });
            $scope.structureGroups.updateSelected();
            Utils.safeApply($scope);
        };

        $scope.openCampaignsDeletion = () => {
            template.open('campaign.lightbox', 'administrator/campaign/campaign-delete-validation');
            $scope.display.lightbox.campaign = true;
        };


        $scope.validCampaignForm = (campaign: Campaign) => {
            return campaign.name !== undefined
            && campaign.name.trim() !== ''
            && _.findWhere($scope.structureGroups.all, {selected: true}) !== undefined;

        };

        $scope.validCampaign = async (campaign: Campaign) => {
            $scope.campaign.groups = [];
            $scope.structureGroups.all.map((group) => $scope.selectCampaignsStructureGroup(group));
            _.uniq($scope.campaign.groups);
            await campaign.save();
            $scope.redirectTo('/campaigns');
            Utils.safeApply($scope);
        };

        $scope.deleteCampaigns = async (campaigns) => {
            await $scope.campaigns.delete(campaigns);
            await $scope.campaigns.sync();
            $scope.allCampaignSelected = false;
            $scope.display.lightbox.campaign = false;
            Utils.safeApply($scope);
        };

        $scope.selectCampaignsStructureGroup = (group) => {
            group.selected ? $scope.campaign.groups.push(group) : $scope.campaign.groups = _.reject($scope.campaign.groups, (groups) => {
                return groups.id === group.id;
            });
        };

        $scope.openStructureGroupForm = (structureGroup: StructureGroup = new StructureGroup()) => {
            $scope.redirectTo('/structureGroups/create');
            $scope.structureGroup = new StructureGroup();
            Mix.extend($scope.structureGroup, structureGroup);
            $scope.structureGroup.structureIdToObject(structureGroup.structures, $scope.structures);
            Utils.safeApply($scope);
        };

        $scope.structuresFilter = (structureRight) => {
            return _.findWhere($scope.structureGroup.structures, {id: structureRight.id}) === undefined;
        };

        $scope.getStructureNumber = () => {
            return _.without($scope.structures.all, ...$scope.structureGroup.structures).length;
        };

        $scope.selectAllStructures = (structures: any) => {
            structures.selectAll();
            Utils.safeApply($scope);
        };

        $scope.deselectAllStructures = (structures: any) => {
            structures.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.updateSelection = (structures: any, value: boolean) => {
            structures.map((structure) => structure.selected = value);
            Utils.safeApply($scope);
        };

        $scope.addStructuresInGroup = () => {
            $scope.structureGroup.structures.push.apply($scope.structureGroup.structures, $scope.structures.selected);
            $scope.structureGroup.structures = _.uniq($scope.structureGroup.structures);
            $scope.structures.deselectAll();
            $scope.search.structure = '';
            Utils.safeApply($scope);
        };

        $scope.deleteStructuresofGroup = () => {
            $scope.structureGroup.structures = _.difference($scope.structureGroup.structures, $scope.structureGroup.structures.filter(structureRight => structureRight.selected));
            $scope.structures.deselectAll();
            $scope.search.structureRight = '';
            Utils.safeApply($scope);
        };

        $scope.validStructureGroupForm = (structureGroup: StructureGroup) => {
            return structureGroup.name !== undefined
                && structureGroup.name.trim() !== ''
                && structureGroup.structures.length > 0;
        };

        $scope.validStructureGroup = async (structureGroup: StructureGroup) => {
            await structureGroup.save();
            $scope.redirectTo('/structureGroups');
            Utils.safeApply($scope);
        };

        $scope.openStructureGroupImporter = (): void => {
            $scope.importer = new StructureGroupImporter();
            template.open('structureGroup.lightbox', 'administrator/structureGroup/structureGroup-importer');
            $scope.display.lightbox.structureGroup = true;
        };

        // noinspection DuplicatedCode
        $scope.importStructureGroups = async (importer: StructureGroupImporter): Promise<void> => {
            try {
                await importer.validate();
            } catch (err) {
                importer.message = err.message;
            } finally {
                if (!importer.message) {
                    $scope.display.lightbox.structureGroup = false;
                    delete $scope.importer;
                } else {
                    importer.files = [];
                }
                await $scope.structureGroups.sync();
                Utils.safeApply($scope);
            }
        };

        $scope.openStructureGroupDeletion = (structureGroup: StructureGroup) => {
            $scope.structureGroup = structureGroup;
            template.open('structureGroup.lightbox', 'administrator/structureGroup/structureGroup-delete');
            $scope.display.lightbox.structureGroup = true;
        };

        $scope.deleteStructureGroup = async () => {
            await $scope.structureGroup.delete();
            await $scope.structureGroups.sync();
            $scope.display.lightbox.structureGroup = false;
            Utils.safeApply($scope);
        };

        $scope.setStatus = async (status: string) => {
            await $scope.equipments.setStatus(status);
            await $scope.equipments.sync();
            $scope.allEquipmentSelected = false;
            toasts.confirm('crre.status.update.ok');
            Utils.safeApply($scope);
        };

/*        $scope.searchOption = async (searchText: string, field: string, model: EquipmentOption, varName: string) => {
            try {
                const options: Equipment[] = await $scope.equipments.search(searchText,field);
                options.map((equipment: Equipment) => {
                    equipment.id_option = equipment.id;
                    delete equipment.id;
                });
                model[varName] = options;
                Utils.safeApply($scope);
            } catch (err) {
                console.error(err);
                model.search = [];
                return;
            }
        };
        $scope.searchOptionByName =(searchText : string, model: EquipmentOption)=>{
            $scope.searchOption(searchText,'name',model, 'search');
        };
        $scope.searchOptionByReference= (searchText: string, model: EquipmentOption) => {
            $scope.searchOption(searchText,'reference',model, 'searchReference');
        };

        $scope.selectOption = function (model: EquipmentOption, option: Equipment) {
            const alreadyAdded = _.findWhere($scope.equipment.options, {id_option: option.id_option});
            if (!alreadyAdded) {
                let index = _.indexOf($scope.equipment.options, model);
                $scope.equipment.options[index] = Mix.castAs(EquipmentOption, {...model, ...option});
                $scope.equipment.options[index].search = undefined;
            }
            Utils.safeApply($scope);
        };*/
    }]);
