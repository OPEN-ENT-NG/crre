import {idiom as lang, ng, toasts} from 'entcore';
import {
    Utils,
    Basket, Equipment,
} from "../../model";

export const historicOrderRegionController = ng.controller('historicOrderRegionController',
    ['$scope', ($scope) => {
        $scope.filter = {
            isOld : false
        };

        $scope.changeOld = async (old: boolean) => {
            if($scope.filter.isOld !== old){
                $scope.filter.isOld = old;
                $scope.filter.page = 0;
                $scope.display.loading = true;
                $scope.projects.all = [];
                Utils.safeApply($scope);
                await $scope.synchroRegionOrders(null, null, $scope.filter.isOld);
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
                $scope.uncheckAll();
                Utils.safeApply($scope);
            }
        };
    }
    ]);