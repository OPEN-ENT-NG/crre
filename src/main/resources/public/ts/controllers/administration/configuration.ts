import {_, ng, template} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {
    Campaign,
    StructureGroup,
    StructureGroupImporter,
    Utils
} from '../../model';

export const configurationController = ng.controller('configurationController',
    ['$scope', ($scope) => {
        $scope.display = {
            lightbox: {
                campaign: false,
                structureGroup: false
            },
            input: {
                group: []
            }
        };

        $scope.search = {};

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
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
            group.selected ? $scope.campaign.groups.push(group) :
                $scope.campaign.groups = _.reject($scope.campaign.groups, (groups) => {
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
            $scope.structureGroup.structures = _.difference($scope.structureGroup.structures,
                $scope.structureGroup.structures.filter(structureRight => structureRight.selected));
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
    }]);