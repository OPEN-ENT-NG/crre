import {ng} from 'entcore';
import {Campaign, Utils} from '../../model';

export const campaignsListController = ng.controller('campaignsListController',
    ['$scope', '$rootScope', ($scope, $rootScope) => {
        $scope.openCampaign = (campaign: Campaign) => {
            if (campaign.accessible) {
                $scope.emitCampaign(campaign);
                $scope.campaign = campaign;
                $scope.redirectTo(`/equipments/catalog`);
                Utils.safeApply($scope);
            }
        };
        $scope.emitCampaign = function(campaign) {
            $scope.$emit('eventEmitedCampaign', campaign);
        };
        $scope.openOrderToMain = (campaign: Campaign) => {
            $scope.redirectTo(`/campaign/${campaign.id}/order`);
            $scope.campaign = campaign;
        };
    }]);