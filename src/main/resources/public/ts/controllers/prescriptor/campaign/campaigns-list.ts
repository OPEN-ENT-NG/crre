import {ng, template} from 'entcore';
import {Campaign, Utils} from '../../../model';

export const campaignsListController = ng.controller('campaignsListController',
    ['$scope', ($scope) => {

        $scope.openCampaign = (campaign: Campaign) => {
            if (campaign.accessible) {
                $scope.$emit('eventEmitedCampaign', campaign);
                $scope.campaign = campaign;
                $scope.redirectTo(`/equipments/catalog/${campaign.id}`);
                Utils.safeApply($scope);
            }
        };

        $scope.openOrderToMain = (campaign: Campaign) => {
            if (campaign.accessible) {
                $scope.$emit('eventEmitedCampaign', campaign);
                $scope.campaign = campaign;
                $scope.redirectTo(`/campaign/${campaign.id}/order`);
                Utils.safeApply($scope);
            }
        };

        $scope.openCampaignWaitingOrder = (campaign: Campaign) => {
            if (campaign.accessible) {
                $scope.$emit('eventEmitedCampaign', campaign);
                $scope.campaign = campaign;
                $scope.redirectTo(`/order/${campaign.id}/waiting`);
                Utils.safeApply($scope);
            }
        };

        $scope.modifyNumberStudent = () => {
            template.open('number.student', 'prescriptor/campaign/modify-number-student');
            $scope.display.lightbox.modifyNumberStudent = true;
            Utils.safeApply($scope);
        };

        $scope.cancelUpdateNumberStudent = () => {
            $scope.display.lightbox.modifyNumberStudent = false;
            template.close('number.student');
            Utils.safeApply($scope);
        };

        $scope.getStudent = async () => {
            await $scope.student.getAmount($scope.current.structure.id);
            await calculateLicence();
            Utils.safeApply($scope);
        };

        const calculateLicence = async () => {
           if($scope.student.pro) {
               $scope.total_licence = ($scope.student.seconde + $scope.student.premiere + $scope.student.terminale) * 3;
           } else {
               $scope.total_licence = $scope.student.seconde * 9 + $scope.student.premiere * 8 + $scope.student.terminale * 7;
           }
        };

        $scope.updateNumberStudent = async (seconde: number, premiere: number, terminale: number) => {
            await $scope.student.updateAmount($scope.current.structure.id, seconde, premiere, terminale, $scope.student.pro, $scope.total_licence);
            await $scope.student.getAmount($scope.current.structure.id);
            await calculateLicence();
            $scope.display.lightbox.modifyNumberStudent = false;
            template.close('number.student');
            Utils.safeApply($scope);
        };

    }]);