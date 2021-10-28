import {ng} from 'entcore';
import {Filter, Filters, Utils} from '../../../model';
import http from "axios";

export const statsStructureController = ng.controller('statsStructureController', [
    '$scope', async ($scope) => {
        $scope.filterChoice = {
            schoolType: [],
            docType: [],
            years: []
        };
        $scope.docsType = [{name: 'Papier'}, {name: 'Numerique'}];
        $scope.schoolType = [{name: 'Public'}, {name: 'Privé'}];

        $scope.schoolType.forEach((item) => item.toString = () => $scope.translate(item.name));
        $scope.docsType.forEach((item) => item.toString = () => $scope.translate(item.name));

        let { data } = await http.get(`/crre/region/statistics/years`);
        $scope.years = data;
        $scope.years.forEach((item) => item.toString = () => $scope.translate(item.name));

        $scope.filterChoiceCorrelation = {
            keys : ["docsType","year", "schoolType"],
            years : 'year',
            schoolType : 'public',
            docsType : 'catalog'
        };


        this.init = async () => {
            // Init the stat for the current year
            let date;
            if(new Date().getMonth() > 4) {
                date = new Date().getFullYear() + 1;
                date = date.toString();
            } else {
                date = new Date().getFullYear().toString();
            }
            let filterYear = new Filter();
            filterYear.name = "year";
            filterYear.value = date;
            $scope.filters = new Filters();
            $scope.filters.all.push(filterYear);
            await $scope.statsStructure.get($scope.filters);
            Utils.safeApply($scope);

            // Init filter as last year
            $scope.filterChoice.years.push($scope.years[0]);
            Utils.safeApply($scope);
        };


        $scope.getFilter = async () => {
            $scope.filters.all = [];
            for (const key of Object.keys($scope.filterChoice)) {
                $scope.filterChoice[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.filterChoiceCorrelation[key];
                    newFilter.value = item.name;
                    $scope.filters.all.push(newFilter);
                });
                Utils.safeApply($scope);
            }
            await $scope.statsStructure.get($scope.filters, $scope.query_name);
            Utils.safeApply($scope);
        }

        $scope.search = async () => {
            await $scope.statsStructure.get($scope.filters, $scope.query_name);
            Utils.safeApply($scope);
        }

        this.init();
        Utils.safeApply($scope);
    }
]);