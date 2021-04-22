import {_, ng} from 'entcore';
import {Equipment, Filter, Filters, Utils} from '../../../model';

export const catalogController = ng.controller('catalogController',
    ['$scope', ($scope) => {
        this.init = () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipment = new Equipment();
            $scope.loading = true;
            $scope.catalog = {
                subjects : [],
                public : [],
                grades : [],
                docsType : [],
                editors : []
            };
            $scope.correlationFilterES = {
                keys : ["subjects","public","grades","docsType","editors"],
                subjects : 'disciplines.libelle',
                public : 'publiccible',
                grades : 'niveaux.libelle',
                docsType : '_index',
                editors : 'editeur'
            };

            if($scope.isAdministrator()){
                $scope.goBackUrl = "crre#/equipments/catalog";
            }else if($scope.hasAccess() && !$scope.isValidator() && !$scope.isPrescriptor()){
                $scope.goBackUrl = "crre#/equipments/catalog/0";
            }else{
                $scope.goBackUrl = "crre#/equipments/catalog/" + $scope.campaign.id;
            }
        };

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
    }]);