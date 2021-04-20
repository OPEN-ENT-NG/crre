import {idiom, idiom as lang, ng, toasts} from 'entcore';
import { Utils} from '../../../model';

export const basketConfirmationNameController = ng.controller('basketConfirmationNameController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {

        $scope.takeClientOrder = async (basket_name: string) => {
            let totalPriceOrder = $scope.calculatePriceOfEquipments($scope.baskets_test, 2);
            totalPriceOrder = parseFloat(totalPriceOrder).toLocaleString(undefined,
                {minimumFractionDigits: 2, maximumFractionDigits: 2});
            let nbrEquipment = $scope.calculateQuantity($scope.baskets_test,true);
            let {status, data} = await $scope.baskets_test.takeOrder(parseInt($routeParams.idCampaign),
                $scope.current.structure, basket_name);
            if(status === 200) {
                confirmOrder(data, nbrEquipment, totalPriceOrder)
                await $scope.baskets_test.sync(parseInt($routeParams.idCampaign), $scope.current.structure.id);
                $scope.cancelConfirmBasketName();
            }
        };

        const confirmOrder = (data, nbr_equipment, totalPriceOrder) => {
            $scope.campaign.nb_order += 1;
            $scope.campaign.order_notification += 1;
            $scope.campaign.nb_order_waiting += nbr_equipment;
            $scope.campaign.nb_panier = 0;
            $scope.campaign.purse_amount = data.amount;
            let message = lang.translate('crre.confirm.order.message1') +
                "<strong>" + totalPriceOrder + " " + idiom.translate('money.symbol') + "</strong>" +
                lang.translate('crre.confirm.order.message2');
            toasts.confirm(message);
            Utils.safeApply($scope);
        };
    }]);