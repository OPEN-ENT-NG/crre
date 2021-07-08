import {_, ng} from 'entcore';
import {
    Campaign,
    Utils
} from '../../../model';
import http from "axios";

export const campaignFormController = ng.controller('campaignFormController',
    ['$scope', ($scope) => {
        this.init = async () => {
            $scope.articleFormat = [{name : "Tous", value: null}, {name: "Catalogue papier", value: "articlepapier"},
                {name: "Catalogue numérique", value: "articlenumerique"}];
            $scope.formatCheck = [];
            $scope.categories = ["Tous", "Papier", "Numérique", "Mixte"];
            await $scope.getTypesCampaign();
            if(!!$scope.campaign.id) {
                $scope.campaign.catalog = $scope.articleFormat.find(format => $scope.campaign.catalog == format.value);
                $scope.campaign.name_type = $scope.formatCheck.find(format => $scope.campaign.id_type == format.id_type).name_type;
                $scope.campaign_type = $scope.formatCheck.find(format => $scope.campaign.id_type == format.id_type);
                Utils.safeApply($scope);
            } else {
                $scope.campaign = new Campaign();
            }
        }


        $scope.changeList = () => {
            $scope.campaign.use_credit = $scope.campaign_type.credit;
            $scope.campaign.reassort = $scope.campaign_type.reassort;
            $scope.campaign.automatic_close = $scope.campaign_type.automatic_close;
            $scope.campaign.id_type = $scope.campaign_type.id_type;
            $scope.campaign.name_type = $scope.campaign_type.name_type;
            $scope.campaign.catalog = $scope.campaign_type.catalog;
            $scope.structureGroups.all.forEach(structure => {
                if(_.contains($scope.campaign_type.structure, structure.name)) {
                    structure.selected = true;
                } else {
                    structure.selected = false;
                }
            });
            $scope.campaign_type.structure
            Utils.safeApply($scope);
        }

        $scope.getTypesCampaign = async () => {
            let {data} = await http.get(`/crre/campaigns/types`);
            data.forEach(function (type) {
                type.catalog = $scope.articleFormat.find(format => type.catalog == format.name);
                type.structure = JSON.parse(type.structure);
                $scope.formatCheck.push(type);
            });
            Utils.safeApply($scope);
        }

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.validCampaignForm = (campaign: Campaign) => {
            return campaign.name !== undefined
                && campaign.name.trim() !== ''
                && _.findWhere($scope.structureGroups.all, {selected: true}) !== undefined;

        };

        const selectCampaignsStructureGroup = (group) => {
            group.selected ? $scope.campaign.groups.push(group) :
                $scope.campaign.groups = _.reject($scope.campaign.groups, (groups) => {
                    return groups.id === group.id;
                });
        };

        $scope.validCampaign = async (campaign: Campaign) => {
            $scope.campaign.groups = [];
            $scope.campaign.catalog = $scope.campaign.catalog.value;
            $scope.structureGroups.all.map((group) => selectCampaignsStructureGroup(group));
            _.uniq($scope.campaign.groups);
            await campaign.save();
            $scope.redirectTo('/campaigns');
            Utils.safeApply($scope);
        };
        this.init();
    }]);