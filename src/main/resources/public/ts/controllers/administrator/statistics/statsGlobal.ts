import {ng} from 'entcore';
import {Utils} from '../../../model';

export const statsGlobalController = ng.controller('statsGlobalController', [
    '$scope', async ($scope) => {

        $scope.reassorts = [{name: 'true'}, {name: 'false'}];
        $scope.schoolOrientation = [{name: 'LG'}, {name: 'LP'}];

        $scope.reassorts.forEach((item) => item.toString = () => $scope.translate(item.name));
        $scope.schoolOrientation.forEach((item) => item.toString = () => $scope.translate(item.name));

        this.init = async () => {
            await $scope.initFilter();
            await $scope.initYear();
            await $scope.stats.get($scope.filters);
            Utils.safeApply($scope);
            // Init filter as last year
            $scope.filterChoice.years.push($scope.years[0]);
            Utils.safeApply($scope);
        };

        $scope.getPublicTotal = (field, publics) => {
            let total = 0;
            if(field.find(r => r.public === publics)) {
                total = field.find(r => r.public === publics).total;
            }
            return total;
        }

        $scope.getPublicPercentage = (field, publics) => {
            let total = 0;
            if(field.find(r => r.public === publics)) {
                total = field.find(r => r.public === publics).percentage;
            }
            return total;
        }

        $scope.isPublic = (publics) => {
            return !!$scope.filterChoice.schoolType.find(r => r.name === publics) || $scope.filterChoice.schoolType.length == 0;
        }

        $scope.getFilter = async () => {
            $scope.getAllFilter();
            await $scope.stats.get($scope.filters);
            Utils.safeApply($scope);
        }

        this.init();
        Utils.safeApply($scope);


    }
]);