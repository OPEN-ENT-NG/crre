import {_, ng} from 'entcore';
import {Equipment, Filter, Filters, Utils} from '../../../model';

export const catalogController = ng.controller('catalogController',
    ['$scope', ($scope) => {
        this.init = async () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipment = new Equipment();
            $scope.loading = true;
            $scope.equipments.consumables = [{name: 'Consommable'}, {name: 'Manuel'}];
            $scope.equipments.consumables.forEach((item) => item.toString = () => $scope.translate(item.name));
            $scope.equipments.pros = [{name: 'Lycée général'}, {name: 'Lycée professionnel'}];
            $scope.equipments.pros.forEach((item) => item.toString = () => $scope.translate(item.name));

            $scope.catalog = {
                subjects: [],
                public: [],
                grades: [],
                docsType: [],
                editors: [],
                consumables: [],
                pros: []
            };

            $scope.preFilter = {
                subjects: [],
                public: [],
                grades: [],
                docsType: [],
                editors: [],
                consumables: [],
                pros: []
            }
            $scope.correlationFilterES = {
                keys: ["subjects", "public", "grades", "docsType", "editors", "consumables", "pros"],
                subjects: 'disciplines.libelle',
                public: 'publiccible',
                grades: 'niveaux.libelle',
                docsType: '_index',
                editors: 'editeur',
                consumables: 'conso',
                pros: 'pro'
            };

            if (!!$scope.campaign.catalog && $scope.filters.all.length == 0) {
                $scope.filters = new Filters();
                // If catalog contain consommable filter
                if ($scope.campaign.catalog.split("|").includes("consommable")) {
                    let consommableFilter = new Filter();
                    consommableFilter.name = "conso";
                    consommableFilter.value = "Consommable";
                    $scope.filters.all.push(consommableFilter);
                }
                if ($scope.campaign.catalog.split("|").includes("pro") || $scope.campaign.catalog.split("|").includes("lgt")) {
                    let proFilter = new Filter();
                    proFilter.name = "pro";
                    proFilter.value = $scope.campaign.catalog.split("|").includes("pro") ? "Lycée professionnel" : "Lycée général";
                    $scope.filters.all.push(proFilter);
                }
                if (["articlenumerique","articlepapier"].indexOf($scope.campaign.catalog.split("|")[0]) != -1) {
                    let catalogFilter = new Filter();
                    catalogFilter.name = "_index";
                    catalogFilter.value = $scope.campaign.catalog.split("|")[0];
                    $scope.filters.all.push(catalogFilter);
                }
            } else {
                $scope.correlationFilterES.keys.forEach(key => {
                    let arrayFilter = [];
                    $scope.filters.all.filter(t => t.name == $scope.correlationFilterES[key]).forEach(filter => {
                        arrayFilter.push($scope.equipments[key].find(c => c.name == filter.value));
                    });
                    $scope.catalog[key] = arrayFilter;
                });
            }
        }

        $scope.addFilter = async () => {
            if (!!$scope.queryWord || $scope.queryWord == '') {
                $scope.query.word = $scope.queryWord;
            }
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            // Add to prefilter to hide filters in front
            if (!!$scope.campaign.catalog) {
                $scope.campaign.catalog.split("|").includes("consommable") ? $scope.preFilter["consumables"].push(true) : null;
                $scope.campaign.catalog.split("|").includes("pro") || $scope.campaign.catalog.split("|").includes("lgt") ? $scope.preFilter["pros"].push(true) : null;
                $scope.preFilter["docsType"].push(true)
            }
            Utils.safeApply($scope);
            await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
            if (!!$scope.campaign.catalog) {
                let arrayDocs = [];
                arrayDocs.push($scope.equipments.docsType.find(c => c.name = $scope.campaign.catalog.split("|")[0]));
                $scope.catalog["docsType"] = arrayDocs;
                if ($scope.campaign.catalog.split("|").includes("consommable")) {
                    let arrayConso = [];
                    arrayConso.push($scope.equipments.consumables.find(c => c.name = "Consommable"));
                    $scope.catalog["consumables"] = arrayConso;
                }
                if ($scope.campaign.catalog.split("|").includes("pro") || $scope.campaign.catalog.split("|").includes("lgt")) {
                    let arrayPro = [];
                    let type = $scope.campaign.catalog.split("|").includes("pro") ? "Lycée professionnel" : "Lycée général";
                    arrayPro.push($scope.equipments.pros.find(t => t.name = type));
                    $scope.catalog["pros"] = arrayPro;
                }
            }
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
            $scope.$emit('eventEmitedQuery', $scope.query.word);
            $scope.$emit('eventEmitedFilters', $scope.filters);
            if ($scope.filters.all.length > 0) {
                await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
                Utils.safeApply($scope);
            } else {
                await $scope.equipments.getFilterEquipments($scope.query.word);
                Utils.safeApply($scope);
            }
        };

        $scope.dropElement = (item, key): void => {
            $scope.filters = $scope.filters.all.filter(f => f.value != item.name);
            if (!!$scope.filters) {
                $scope.filters = new Filters();
            }
            $scope.$emit('eventEmitedFilters', $scope.filters);
            $scope.catalog[key] = _.without($scope.catalog[key], item);
            $scope.getFilter();
        };

        this.init();
        $scope.addFilter();
    }]);