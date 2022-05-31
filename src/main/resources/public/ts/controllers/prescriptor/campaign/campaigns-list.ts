import {ng, template} from 'entcore';
import {Campaign, Filter, Student, Utils} from '../../../model';

export const campaignsListController = ng.controller('campaignsListController',
    ['$scope', ($scope) => {

        $scope.openCampaign = (campaign: Campaign) => {
            if (campaign.accessible) {
                $scope.$emit('eventEmitedCampaign', campaign);
                $scope.campaign = campaign;
                // Reset campaign without filter
                $scope.query.word = '';
                $scope.filters.all = [];
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

        $scope.openWaitingOrder = () => {
            $scope.redirectTo(`/order/waiting`);
            Utils.safeApply($scope);
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
               $scope.total_licence = (($scope.student.secondepro + $scope.student.premierepro + $scope.student.terminalepro +
                       $scope.student.cap1 + $scope.student.cap2 + $scope.student.cap3 + $scope.student.bma1 + $scope.student.bma2) * 3) +
                   $scope.student.seconde * 9 + $scope.student.premiere * 8 + $scope.student.terminale * 7;
        };

        $scope.updateNumberStudent = async (student: Student) => {
            await $scope.student.updateAmount($scope.current.structure.id, student, $scope.total_licence);
            await $scope.student.getAmount($scope.current.structure.id);
            await calculateLicence();
            $scope.display.lightbox.modifyNumberStudent = false;
            template.close('number.student');
            Utils.safeApply($scope);
        };

    }]);