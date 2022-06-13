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
            purse.amount = $scope.purses.selected[0].amount +
                (purse.initial_amount - $scope.purses.selected[0].initial_amount);
            purse.consumable_amount = $scope.purses.selected[0].consumable_amount +
                (purse.consumable_initial_amount - $scope.purses.selected[0].consumable_initial_amount);
            purse.licence_amount = $scope.purses.selected[0].licence_amount +
                (purse.licence_initial_amount - $scope.purses.selected[0].licence_initial_amount);
            purse.consumable_licence_amount = $scope.purses.selected[0].consumable_licence_amount +
                (purse.consumable_licence_initial_amount - $scope.purses.selected[0].consumable_licence_initial_amount);
            purse.selected = false;
            $scope.purses.all = $scope.purses.all.filter(purse => { return purse.id != $scope.purses.selected[0].id });
            $scope.purses.push(purse);
            $scope.purses.forEach(purse => {purse.selected = false;});
            $scope.allPurseSelected = false;
            Utils.safeApply($scope);
        };

        $scope.checkPurse = () => {
            return ($scope.purses.selected[0].initial_amount && !$scope.purse.initial_amount) ||
                ($scope.purses.selected[0].consumable_initial_amount && !$scope.purse.consumable_initial_amount);
        }
    }]);