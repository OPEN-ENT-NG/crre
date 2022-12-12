import {_, ng} from 'entcore';
import {Utils} from '../../../model';

export const statsStructureController = ng.controller('statsStructureController', [
    '$scope', async ($scope) => {

        $scope.consummation = [{name: '0', order: 0}, {name: '20', order: 1}, {name: '40', order: 2}, {name: '60', order: 3},
            {name: '80', order: 4}, {name: '100', order: 5}];
        $scope.consummation.forEach((item) => item.toString = () => $scope.translate(item.name));
        Utils.safeApply($scope);

        const init = async () => {
            await $scope.initFilter();
            await $scope.initYear();
            await $scope.statsStructure.get($scope.filters);
            Utils.safeApply($scope);
            // Init filter as last year
            $scope.filterChoice.years.push($scope.years[0]);

            $scope.cities = getUnique("city");
            $scope.regions = getUnique("region");

            Utils.safeApply($scope);

        };


        $scope.getFilter = async () => {
            $scope.getAllFilter();
            await $scope.statsStructure.get($scope.filters, $scope.query_name);
            Utils.safeApply($scope);
        }

        $scope.search = async () => {
            await $scope.statsStructure.get($scope.filters, $scope.query_name);
            Utils.safeApply($scope);
        }

        function getUnique(type: string) {
            let value = $scope.statsStructure.all
                .map(p => {
                    p = {"name": type == "city" ? p.city : p.region};
                    return p;
                });
            value = _.uniq(value, x => x.name);
            value.forEach((item) => item.toString = () => $scope.translate(item.name));
            return value;
        }

        await init();
        Utils.safeApply($scope);

    }


]);