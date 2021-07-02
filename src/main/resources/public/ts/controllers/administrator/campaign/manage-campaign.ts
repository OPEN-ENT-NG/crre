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
                automaticCampaign:false
            }
        };

        $scope.updateAccessibility = async (campaign: Campaign) => {
            $scope.automaticCampaign = campaign;
            if (campaign.automatic_close) {
                template.open('campaign.lightbox.automaticCampaign', 'administrator/campaign/campaign-change-manual');
                $scope.display.lightbox.automaticCampaign = true;
            } else {
                $scope.loadingArray = true;
                Utils.safeApply($scope);
                await campaign.updateAccessibility();
                await $scope.campaigns.sync();
                $scope.allCampaignSelected = false;
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            }
        }

        $scope.openCampaignForm = async (campaign: Campaign = new Campaign()) => {
            let id = campaign.id;
            // Create if id not found
            if(!!!id) {
                $scope.campaign = new Campaign();
                Mix.extend($scope.campaign, campaign);
            }
            await $scope.structureGroups.sync();
            id ? await updateSelectedCampaign(id) : null;
            id ? $scope.redirectTo('/campaigns/update') : $scope.redirectTo('/campaigns/create');
            Utils.safeApply($scope);
        };

        const updateSelectedCampaign = async (id) => {
            await $scope.campaign.sync(id);
            $scope.structureGroups.all = $scope.structureGroups.all.map((group) => {
                let Cgroup = _.findWhere($scope.campaign.groups, {id: group.id});
                if (Cgroup !== undefined) {
                    group.selected = true
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