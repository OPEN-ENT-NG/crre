import {_, ng, template} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {
    Campaign,
    Utils
} from '../../../model';

export const campaignsController = ng.controller('campaignsController',
    ['$scope', ($scope) => {
        $scope.display = {
            lightbox: {
                campaign: false,
            }
        };

        $scope.openCampaignForm = async (campaign: Campaign = new Campaign()) => {
            let id = campaign.id;
            id ? $scope.redirectTo('/campaigns/update') : $scope.redirectTo('/campaigns/create');
            $scope.campaign = new Campaign();
            Mix.extend($scope.campaign, campaign);
            await $scope.structureGroups.sync();
            id ? await updateSelectedCampaign(id) : null;
            Utils.safeApply($scope);
        };

        const updateSelectedCampaign = async (id) => {
            await $scope.campaign.sync(id, $scope.tags.all);
            $scope.structureGroups.all = $scope.structureGroups.all.map((group) => {
                let Cgroup = _.findWhere($scope.campaign.groups, {id: group.id});
                if (Cgroup !== undefined) {
                    group.select();
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
    }]);