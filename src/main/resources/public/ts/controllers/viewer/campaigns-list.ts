import {ng} from 'entcore';
import {Campaign, Utils} from '../../model';

export const campaignsListController = ng.controller('campaignsListController',
    ['$scope', '$rootScope', ($scope, $rootScope) => {
        $scope.openCampaign = (campaign: Campaign) => {
            if (campaign.accessible) {
                $scope.emitCampaign(campaign);
                $scope.redirectTo(`/campaign/${campaign.id}/catalog`);
                Utils.safeApply($scope);
            }
        };
        $scope.emitCampaign = function(campaign) {
            $scope.$emit('eventEmitedCampaign', campaign);
        };
        $scope.openOrderToMain = (campaign: Campaign) => {
            $scope.redirectTo(`/campaign/${campaign.id}/order`);
            $scope.campaign.accessible= campaign.accessible;
            $scope.campaign.description= campaign.description;
            $scope.campaign.groups= campaign.groups;
            $scope.campaign.id= campaign.id;
            $scope.campaign.image= campaign.image;
            $scope.campaign.name= campaign.name;
            $scope.campaign.purse_amount= campaign.purse_amount;
            $scope.campaign.nb_panier= campaign.nb_panier;
            $scope.campaign.nb_structures= campaign.nb_structures;
            $scope.campaign.priority_enabled= campaign.priority_enabled;
            $scope.campaign.priority_field= campaign.priority_field;
            $scope.campaign.purse_enabled= campaign.purse_enabled;
        };
    }]);