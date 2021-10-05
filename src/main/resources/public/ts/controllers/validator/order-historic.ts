import {idiom as lang, ng, toasts} from 'entcore';
import {
    Utils,
    Basket, Equipment, FilterFront, Filter,
} from "../../model";
import http from "axios";

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
                await $scope.updateAllStatus();
                $scope.filter.isOld = old;
                $scope.filter.page = 0;
                $scope.filtersFront.all = [];
                $scope.filters.all = []
                $scope.display.loading = true;
                $scope.projects.all = [];
                Utils.safeApply($scope);
                await $scope.synchroRegionOrders(null, null, $scope.filter.isOld, $scope.filter.page);
            }
        };

        $scope.reSubmit = async () => {
            let statusOK = true;
            let totalAmount = 0;
            let promesses = []

            function prepareAndCreateOrder() {
                $scope.projects.all.forEach(project => {
                    project.orders.forEach(async order => {
                        if (order.selected) {
                            let equipment = new Equipment();
                            equipment.ean = order.equipment_key;
                            let basket = new Basket(equipment, $scope.campaign.id, $scope.current.structure.id);
                            basket.amount = order.amount;
                            totalAmount += order.amount;
                            promesses.push(basket.create());
                        }
                    });
                });
            }

            prepareAndCreateOrder();
            let responses = await Promise.all(promesses);

            function notify() {
                if (statusOK) {
                    let messageForMany = totalAmount + ' ' + lang.translate('articles') + ' ' +
                        lang.translate('crre.basket.added.articles');
                    toasts.confirm(messageForMany);
                } else {
                    toasts.warning('crre.basket.added.articles.error')
                }
            }

            function newElementTab() {
                for (let i = 0; i < responses.length; i++) {
                    if (responses[i].status === 200) {
                        if ($scope.campaign.nb_panier)
                            $scope.campaign.nb_panier += 1;
                        else
                            $scope.campaign.nb_panier = 1;
                    } else {
                        statusOK = false;
                    }
                }
            }

            if (responses[0].status) {
                newElementTab();
                notify();
                uncheckAll();
                Utils.safeApply($scope);
            }
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
                await $scope.searchProjectAndOrders($scope.filter.isOld);
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

        $scope.updateAllStatus = async () => {
            let {data} = await http.get('/crre/region/orders/old/status');
            return data;
        };

        $scope.getColor = (id) => {
            let color = "";
            switch (id)
            {
                case 0:
                    color = "SENT";
                    break;
                case 1:
                    color = "NEW";
                    break;
                case 2:
                    color = "GRAY";
                    break;
                case 3:
                    color = "GRAY";
                    break;
                case 4:
                    color = "GRAY";
                    break;
                case 6:
                    color = "GRAY";
                    break;
                case 7:
                    color = "GRAY";
                    break;
                case 9:
                    color = "GRAY";
                    break;
                case 10:
                    color = "DONE";
                    break;
                case 14:
                    color = "WAITING_FOR_ACCEPTANCE";
                    break;
                case 15:
                    color = "REJECTED";
                    break;
                case 20:
                    color = "REJECTED";
                    break;
                case 35:
                    color = "WAITING_FOR_ACCEPTANCE";
                    break;
                case 55:
                    color = "SENT";
                    break;
                case 57:
                    color = "SENT";
                    break;
                case 58:
                    color = "BLUE";
                    break;
                case 59:
                    color = "REJECTED";
                    break;
            }
            return color;
        };


    }
    ]);