import {ng} from 'entcore';
import {Filter, Filters, Utils} from '../../../model';
import http from "axios";

export const statsController = ng.controller('statsController', ['$scope', async ($scope) => {

        $scope.filterChoice = {
            schoolType: [],
            docType: [],
            cities: [],
            regions: [],
            years: [],
            reassorts: [],
            schoolOrientation: [],
            consummation: []
        };

        $scope.filterChoiceCorrelation = {
            keys: ["catalogs", "reassorts", "year", "schoolType", "schoolOrientation", "cities", "regions", "consummation"],
            years: 'year',
            schoolType: 'public',
            catalogs: 'catalog',
            reassorts: 'reassort',
            schoolOrientation: 'orientation',
            cities: 'city',
            regions: 'region',
            consummation: 'consummation'
        };

        $scope.catalogs = [{name: 'Papier'}, {name: 'Numerique'}];
        $scope.schoolType = [{name: 'Public'}, {name: 'Privé'}];

        $scope.schoolType.forEach((item) => item.toString = () => $scope.translate(item.name));
        $scope.catalogs.forEach((item) => item.toString = () => $scope.translate(item.name));

        $scope.initFilter = async () => {
            // Init the stat for the current year
            let date;
            if (new Date().getMonth() > 4) {
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
        }

        $scope.initYear = async () => {
            let {data} = await http.get(`/crre/region/statistics/years`);
            if (data.length === 0) {
                let date;
                if (new Date().getMonth() > 4) {
                    date = new Date().getFullYear() + 1;
                    date = date.toString();
                } else {
                    date = new Date().getFullYear().toString();
                }
                $scope.years = [{name: date}];
            } else {
                $scope.years = data;
            }
            $scope.years.forEach((item) => item.toString = () => $scope.translate(item.name));
        }

        $scope.getAllFilter = async () => {
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
        }
    }
    ])
;