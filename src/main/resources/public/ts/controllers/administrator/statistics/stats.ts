import {moment, ng, idiom as lang, angular} from 'entcore';
import {Filter, FilterFront, Filters, Utils} from '../../../model';
import {Statistics} from "../../../model/Statistics";
import http from "axios";

export const statsController = ng.controller('statsController', [
    '$scope', async ($scope) => {
        $scope.filterChoice = {
            schoolType: [],
            docType: [],
            reassorts: [],
            years: [],
        };
        $scope.docsType = [{name: 'Papier'}, {name: 'Numerique'}];
        $scope.schoolType = [{name: 'Public'}, {name: 'Privé'}];

        $scope.schoolType.forEach((item) => item.toString = () => $scope.translate(item.name));
        $scope.docsType.forEach((item) => item.toString = () => $scope.translate(item.name));

        $scope.reassorts = [{name: 'true'}, {name: 'false'}];
        $scope.reassorts.forEach((item) => item.toString = () => $scope.translate(item.name));

        let { data } = await http.get(`/crre/region/statistics/years`);
        $scope.years = data;
        $scope.years.forEach((item) => item.toString = () => $scope.translate(item.name));

        $scope.filterChoiceCorrelation = {
            keys : ["docsType","reassorts","year", "schoolType"],
            years : 'year',
            schoolType : 'public',
            docsType : 'catalog',
            reassorts : 'reassort'
        };


        $scope.init = async () => {
            // Init filter as last year and reassort false
            $scope.filterChoice.years.push($scope.years[0]);
            $scope.filterChoice.reassorts.push($scope.reassorts[1]);
            Utils.safeApply($scope);
        };

        $scope.getPublic = (field, publics) => {
            return field.find(r => r.public === publics).total;
        }

        $scope.isPublic = (publics) => {
            return !!$scope.filterChoice.schoolType.find(r => r.name === publics) || $scope.filterChoice.schoolType.length == 0;
        }

        $scope.computeAllEtab = () => {
            let total = 0;
            if($scope.stats.structures.length > 1) {
                total = $scope.stats.structures[0].total + $scope.stats.structures[1].total;
            } else {
                total = $scope.stats.structures[0].total;
            }
            return total;
        }

        $scope.computeAllEtabMoreThanOneOrder = () => {
            let total = 0;
            if($scope.stats.structuresMoreOneOrder.length > 1) {
                total = $scope.stats.structuresMoreOneOrder[0].total + $scope.stats.structuresMoreOneOrder[1].total;
            } else {
                total = $scope.stats.structuresMoreOneOrder[0].total;
            }
            return total;
        }

        $scope.computeAllRessources = (publics) => {
            return $scope.getPublic($scope.stats.allNumericRessources, publics) + $scope.getPublic($scope.stats.allPaperRessources, publics);
        }

        $scope.computePercentageStructure = (publics) => {
            return ($scope.getPublic($scope.stats.structuresMoreOneOrder, publics) / $scope.getPublic($scope.stats.structures, publics) * 100) + " %";
        }

        $scope.computePercentageAllStructure = () => {
            return ($scope.computeAllEtabMoreThanOneOrder() / $scope.computeAllEtab() * 100) + " %";
        }

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
            await $scope.stats.get($scope.filters);
            Utils.safeApply($scope);
        }

        angular.element(document).ready(function () {
            $scope.init();
            Utils.safeApply($scope);
        });
    }
]);