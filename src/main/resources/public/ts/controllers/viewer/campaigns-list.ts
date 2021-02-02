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

        $scope.getStudent = async () => {
            await $scope.student.getAmount($scope.current.structure.id);
            await $scope.calculateLicence();
            Utils.safeApply($scope);
        };

        $scope.calculateLicence = async () => {
           if($scope.student.pro) {
               $scope.total_licence = ($scope.student.Seconde + $scope.student.Premiere + $scope.student.Terminale) * 3;
           } else {
               $scope.total_licence = $scope.student.Seconde * 9 + $scope.student.Premiere * 8 + $scope.student.Terminale * 7;
           }
        };

        $scope.updateNumberStudent = async (seconde: number, premiere: number, terminale: number) => {
            await $scope.student.updateAmount($scope.current.structure.id, seconde, premiere, terminale, $scope.student.pro);
            await $scope.student.getAmount($scope.current.structure.id);
            await $scope.calculateLicence();
            $scope.display.lightbox.modifyNumberStudent = false;
            template.close('number.student');
            Utils.safeApply($scope);
        }
    }]);