import {_, ng} from 'entcore';
import {
    Campaign,
    Utils
} from '../../../model';

export const campaignFormController = ng.controller('campaignFormController',
    ['$scope', ($scope) => {

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
            $scope.structureGroups.all.map((group) => selectCampaignsStructureGroup(group));
            _.uniq($scope.campaign.groups);
            await campaign.save();
            $scope.redirectTo('/campaigns');
            Utils.safeApply($scope);
        };

    }]);