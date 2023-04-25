import {idiom, idiom as lang, ng, toasts} from 'entcore';
import {Utils} from '../../../model';
import {I18nUtils} from "../../../utils/i18n.utils";

export const basketConfirmationNameController = ng.controller('basketConfirmationNameController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {

        $scope.takeClientOrder = async (basket_name: string) => {
            let totalPriceOrder: string = Utils.calculatePriceOfEquipments($scope.baskets_test, 2);
            totalPriceOrder = parseFloat(totalPriceOrder).toLocaleString(undefined,
                {minimumFractionDigits: 2, maximumFractionDigits: 2});
            let nbrEquipment: number = $scope.calculateQuantity($scope.baskets_test, true);
            let {status, data} = await $scope.baskets_test.takeOrder($routeParams.idCampaign,
                $scope.current.structure, basket_name);
            if (status === 200) {
                confirmOrder(data, nbrEquipment, totalPriceOrder)
                await $scope.baskets_test.sync(parseInt($routeParams.idCampaign), $scope.current.structure.id);
                $scope.cancelConfirmBasketName();
                if ($scope.isValidatorInStructure($scope.current.structure)) {
                    toasts.info('crre.confirm.basket.validator');
                    $scope.redirectTo(`/structure/${$scope.current.structure.id}/order/waiting`);
                }
            }
        };

        $scope.getQuantity = (amount: number): string => {
            return I18nUtils.getWithParams("crre.confirm.basket.quantity", [
                amount.toString()
            ]);
        }

        const confirmOrder = (data, nbr_equipment: number, totalPriceOrder: string) => {
            $scope.campaign.nb_order += 1;
            $scope.campaign.order_notification += 1;
            $scope.campaign.nb_order_waiting += nbr_equipment;
            $scope.campaign.nb_panier = 0;
            if ($scope.campaign.use_credit == "credits") {
                $scope.campaign.purse_amount = data.amount;
            } else if ($scope.campaign.use_credit == "consumable_credits") {
                $scope.campaign.consumable_purse_amount = data.amount;
            }
            let message = lang.translate('crre.confirm.order.message1') +
                "<strong>" + totalPriceOrder + " " + idiom.translate('money.symbol') + "</strong>" +
                lang.translate('crre.confirm.order.message2');
            toasts.confirm(message);
            Utils.safeApply($scope);
        };
    }]);