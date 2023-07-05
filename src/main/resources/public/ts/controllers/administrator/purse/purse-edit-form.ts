import {ng} from 'entcore';
import {Purse, Utils} from '../../../model';

export const purseEditFormController = ng.controller('purseEditFormController',
    ['$scope', '$routeParams', ($scope) => {

        $scope.cancelPurseForm = () => {
            $scope.lightbox.open = false;
            delete $scope.purse;
        };

        $scope.validPurse = async (purse: Purse) => {
            await purse.save();
            $scope.lightbox.open = false;

            if ($scope.purses.selected.length > 0) {
                $scope.purses.selected[0].amount += purse.initial_amount - $scope.purses.selected[0].initial_amount;
                $scope.purses.selected[0].consumable_amount += purse.consumable_initial_amount - $scope.purses.selected[0].consumable_initial_amount;

                $scope.purses.selected[0].added_initial_amount += purse.initial_amount - $scope.purses.selected[0].initial_amount;
                $scope.purses.selected[0].added_consumable_initial_amount += purse.consumable_initial_amount - $scope.purses.selected[0].consumable_initial_amount;

                $scope.purses.selected[0].initial_amount = purse.initial_amount;
                $scope.purses.selected[0].consumable_initial_amount = purse.consumable_initial_amount;
            }

            $scope.purses.forEach(purse => {purse.selected = false;});
            $scope.allPurseSelected = false;
            Utils.safeApply($scope);
        };

        $scope.checkPurse = () => {
            return ($scope.purses.selected[0].initial_amount && !$scope.purse.initial_amount) ||
                ($scope.purses.selected[0].consumable_initial_amount && !$scope.purse.consumable_initial_amount);
        }
    }]);