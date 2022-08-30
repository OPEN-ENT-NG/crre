import {idiom as lang, ng, toasts} from 'entcore';
import {
    Utils,
    Basket, Equipment, FilterFront, Filter, Baskets,
} from "../../model";

export const historicOrderRegionController = ng.controller('historicOrderRegionController',
    ['$scope', async ($scope) => {
        $scope.filter = {
            isOld: false
        };
        $scope.filterChoice = {
            renew: []
        };
        $scope.filterChoiceCorrelation = {
            keys: ["renew"],
            renew: 'renew'
        };
        $scope.renews = [{name: 'true'}, {name: 'false'}];
        $scope.renews.forEach((item) => item.toString = () => $scope.translate(item.name));


        $scope.changeOld = async (old: boolean) => {
            if ($scope.filter.isOld !== old) {
                // Comment until dev of status with LDE
                // await $scope.updateAllStatus();
                $scope.filter.isOld = old;
                $scope.filter.page = 0;
                $scope.filtersFront.all = [];
                $scope.filters.all = []
                $scope.display.loading = true;
                $scope.projects.all = [];
                Utils.safeApply($scope);
                await $scope.synchroRegionOrders(null,false, null, $scope.filter.isOld, $scope.filter.page);
            }
        };

        $scope.reSubmit = async () => {
            let totalAmount = 0;
            let baskets = new Baskets();
            $scope.projects.all.forEach(project => {
                project.orders.forEach(async order => {
                    if (order.selected) {
                        let equipment = new Equipment();
                        equipment.ean = order.equipment_key;
                        let basket = new Basket(equipment, $scope.campaign.id, $scope.current.structure.id);
                        basket.amount = order.amount;
                        totalAmount += order.amount;
                        baskets.push(basket);
                    }
                });
            });

            baskets.create()
                .then(res => {
                    if(res.status === 200){
                        if ($scope.campaign.nb_panier)
                            $scope.campaign.nb_panier += baskets.length;
                        else
                            $scope.campaign.nb_panier = baskets.length;
                        let messageForMany = totalAmount + ' ' + lang.translate('articles') + ' ' +
                            lang.translate('crre.basket.added.articles');
                        toasts.confirm(messageForMany);
                        uncheckAll();
                        Utils.safeApply($scope);
                    } else {
                        toasts.warning('crre.basket.added.articles.error');
                    }
                })
        };

        $scope.getFilter = async () => {
            $scope.filters.all = [];
            $scope.filtersFront.all = [];
            for (const key of Object.keys($scope.filterChoice)) {
                let newFilterFront = new FilterFront();
                newFilterFront.name = $scope.filterChoiceCorrelation[key];
                newFilterFront.value = [];
                $scope.filterChoice[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.filterChoiceCorrelation[key];
                    let value = item.name;
                    newFilter.value = value;
                    newFilterFront.value.push(value);
                    $scope.filters.all.push(newFilter);
                });
                $scope.filtersFront.all.push(newFilterFront);
            }
            if ($scope.filters.all.length > 0) {
                await $scope.searchProjectAndOrders($scope.filter.isOld, false, false);
            } else {
                await $scope.searchByName($scope.query_name, $scope.filter.isOld);
            }
        };

        const uncheckAll = () => {
            $scope.projects.all.forEach(project => {
                project.selected = false;
                project.orders.forEach(async order => {
                    order.selected = false;
                });
            });
            $scope.display.toggle = false;
            Utils.safeApply($scope);
        };
    }
    ]);