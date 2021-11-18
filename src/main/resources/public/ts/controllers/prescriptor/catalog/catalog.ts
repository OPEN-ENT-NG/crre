import {_, ng} from 'entcore';
import {Equipment, Filter, Filters, Utils} from '../../../model';

export const catalogController = ng.controller('catalogController',
    ['$scope', ($scope) => {
        this.init = async () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipment = new Equipment();
            $scope.loading = true;
            $scope.isConso = false;
            $scope.equipments.consumables = [{name: 'true'}, {name: 'false'}];
            $scope.equipments.consumables.forEach((item) => item.toString = () => $scope.translate(item.name));

            $scope.catalog = {
                subjects: [],
                public: [],
                grades: [],
                docsType: [],
                editors: [],
                consumables: []
            };
            $scope.correlationFilterES = {
                keys: ["subjects", "public", "grades", "docsType", "editors", "consumables"],
                subjects: 'disciplines.libelle',
                public: 'publiccible',
                grades: 'niveaux.libelle',
                docsType: '_index',
                editors: 'editeur',
                consumables: 'conso'
            };

            if (!!$scope.campaign.catalog && $scope.filters.all.length == 0) {
                $scope.filters = new Filters();
                let catalogFilter = new Filter();
                catalogFilter.name = "_index";
                // If catalog contain consommable filter
                if (new RegExp('consommable').test($scope.campaign.catalog)) {
                    $scope.isConso = true;
                    let consommableFilter = new Filter();
                    consommableFilter.name = "conso";
                    consommableFilter.value = "true";
                    $scope.filters.all.push(consommableFilter);
                }
                catalogFilter.value = $scope.campaign.catalog.replace("consommable", "");
                $scope.filters.all.push(catalogFilter);
            } else {
                $scope.correlationFilterES.keys.forEach(key => {
                    let arrayFilter = [];
                    $scope.filters.all.filter(t => t.name == $scope.correlationFilterES[key]).forEach(filter => {
                        arrayFilter.push($scope.equipments[key].find(c => c.name = filter.value));
                    });
                    $scope.catalog[key] = arrayFilter;
                });
            }
        }

        $scope.addFilter = async () => {
            $scope.query.word = $scope.queryWord;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
            if ($scope.isConso) {
                let arrayConso = [];
                arrayConso.push($scope.equipments.consumables.find(c => c.name = "true"));
                $scope.catalog["consumables"] = arrayConso;
            }
            let arrayDocs = [];
            arrayDocs.push($scope.equipments.docsType.find(c => c.name = $scope.campaign.catalog.replace("consommable", "")));
            $scope.catalog["docsType"] = arrayDocs;
            Utils.safeApply($scope);
        };

        $scope.getFilter = async () => {
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            $scope.filters = new Filters();
            for (const key of Object.keys($scope.catalog)) {
                $scope.catalog[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.correlationFilterES[key];
                    newFilter.value = item.name;
                    $scope.filters.all.push(newFilter);
                })
            }
            if ($scope.filters.all.length > 0) {
                $scope.$emit('eventEmitedFilters', $scope.filters);
                await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
                Utils.safeApply($scope);
            } else {
                await $scope.equipments.getFilterEquipments($scope.query.word);
                Utils.safeApply($scope);
            }
        };

        $scope.dropElement = (item, key): void => {
            $scope.catalog[key] = _.without($scope.catalog[key], item);
            $scope.getFilter();
        };

        this.init();
        $scope.addFilter();
    }]);