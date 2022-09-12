import {idiom as lang, ng, toasts} from 'entcore';
import {
    Utils,
    Basket, Equipment, FilterFront, Filter, Baskets, Campaign,
} from "../../model";
import {Mix} from "entcore-toolkit";

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

        async function createOrderClient(basketsPerCampaign: {}, totalAmount: number) {
            const current_date = Utils.getCurrentDate();

            let promesses = [];
            for (const idCampaign of Object.keys(basketsPerCampaign)) {
                const baskets: Baskets = basketsPerCampaign[idCampaign];
                const panier: string = lang.translate('crre.basket.resubmit') + current_date;
                promesses.push(baskets.takeOrder(idCampaign.toString(), $scope.current.structure, panier));
            }

            const responses = await Promise.all(promesses);

            if (responses.filter(r => r.status === 200).length === promesses.length) {
                if ($scope.campaign.nb_panier)
                    $scope.campaign.nb_panier += promesses.length;
                else
                    $scope.campaign.nb_panier = promesses.length;
                let messageForMany = totalAmount + ' ' + lang.translate('articles') + ' ' +
                    lang.translate('crre.basket.added.articles');
                toasts.confirm(messageForMany);
                uncheckAll();
                Utils.safeApply($scope);
            } else {
                toasts.warning('crre.basket.added.articles.error');
            }
        }

        $scope.reSubmit = async () => {
            let totalAmount = 0;
            let baskets = new Baskets();
            let idCampaign = undefined;
            $scope.projects.all.forEach(project => {
                project.orders.forEach(async order => {
                    if (order.selected) {
                        const campaign = Mix.castAs(Campaign, JSON.parse(order.campaign.toString()));
                        let equipment = new Equipment();
                        equipment.ean = order.equipment_key;
                        let basket = new Basket(equipment, campaign.id, $scope.current.structure.id);
                        basket.amount = order.amount;
                        basket.selected = true;
                        totalAmount += order.amount;
                        baskets.push(basket);
                    }
                });
            });

            const response = await baskets.create();

            let basketsPerCampaign = {};

            if (response.status === 200) {
                response.data.forEach(basketReturn => {
                    let basket = new Basket();
                    basket.id = basketReturn.id;
                    basket.selected = true;
                    idCampaign = basketReturn.idCampaign;
                    basketsPerCampaign[idCampaign] == undefined ? basketsPerCampaign[idCampaign] = new Baskets() : null;
                    basketsPerCampaign[idCampaign].push(basket);
                })
                await createOrderClient(basketsPerCampaign, totalAmount);
            } else {
                toasts.warning('crre.basket.added.articles.error');
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