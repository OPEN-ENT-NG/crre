import {_, ng} from 'entcore';
import {Equipment, Filter, Filters, Utils} from '../../../model';

export const catalogController = ng.controller('catalogController',
    ['$scope', ($scope) => {
        this.init = async () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipment = new Equipment();
            $scope.loading = true;
            if (!!$scope.campaign.catalog) {
                $scope.filters = new Filters();
                let catalogFilter = new Filter();
                catalogFilter.name = "_index";
                // If catalog contain consommable filter
                if (new RegExp('consommable').test($scope.campaign.catalog)) {
                    let consommableFilter = new Filter();
                    consommableFilter.name = "conso";
                    consommableFilter.value = "true";
                    $scope.filters.all.push(consommableFilter);
                }
                catalogFilter.value = $scope.campaign.catalog.replace("consommable", "");
                $scope.filters.all.push(catalogFilter);
            }
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
                consumables : 'conso'
            };
        }

        $scope.addFilter = async () => {
            $scope.query.word = $scope.queryWord;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
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
            if($scope.filters.all.length > 0) {
                await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
                Utils.safeApply($scope);
            } else {
                await $scope.equipments.getFilterEquipments($scope.query.word);
                Utils.safeApply($scope);
            }
        };

        $scope.dropElement = (item,key): void => {
            $scope.catalog[key] = _.without($scope.catalog[key], item);
            $scope.getFilter();
        };

        this.init();
        $scope.addFilter();
    }]);