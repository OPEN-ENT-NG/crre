import {ng, template} from 'entcore';
import {Campaign, Utils} from '../../model';

export const campaignsListController = ng.controller('campaignsListController',
    ['$scope', ($scope) => {
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
        $scope.modifyNumberStudent = () => {
            template.open('number.student', 'customer/campaign/modify-number-student');
            $scope.display.lightbox.modifyNumberStudent = true;
            Utils.safeApply($scope);
        };
        $scope.cancelUpdateNumberStudent = () => {
            $scope.display.lightbox.modifyNumberStudent = false;
            template.close('number.student');
            Utils.safeApply($scope);
        };
    }]);