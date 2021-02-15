import {_, idiom, idiom as lang, ng, template, toasts} from 'entcore';
import {Basket, Baskets, Utils} from '../../model';

export const basketController = ng.controller('basketController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.display = {
            equipmentOption : [],
            lightbox : {
                deleteBasket : false,
                confirmOrder: false,
                createProject: false,
                addDocuments: false
            },
            grade: "",
        };

        $scope.hasOneSelected = (baskets: Baskets) => {
            let hasSelected = false;
            baskets.all.map((basket) => {
                if (basket.selected) {
                    hasSelected = true;
                }
            });
            return hasSelected;
        };
        $scope.calculatePriceOfEquipments = (baskets: Baskets, roundNumber?: number) => {
            let totalPrice = 0;
            baskets.all.map((basket) => {
                if (basket.equipment.disponibilite[0].valeur !== 'DISPONIBLE') return false;
                if (!$scope.hasOneSelected(baskets) || basket.selected) {
                    let basketItemPrice = $scope.calculatePriceOfBasket(basket,2);
                    totalPrice += !isNaN(basketItemPrice) ? parseFloat(basketItemPrice) : 0;
                }
            });
            return (!isNaN(totalPrice)) ? (roundNumber ? totalPrice.toFixed(roundNumber) : totalPrice ) : '';
        };

        $scope.calculateQuantityOfBasket = (baskets: Baskets) => {
            let quantity = 0;
            baskets.all.map((basket) => {
                if (basket.equipment.disponibilite[0].valeur !== 'DISPONIBLE') return false;
                if (!$scope.hasOneSelected(baskets) || basket.selected) {
                    quantity += basket.amount;
                }
            });
            return quantity;
        };

        $scope.calculatePriceOfBasket = (basket: Basket, roundNumber?: number, toDisplay?: boolean) => {
            let equipmentPrice = $scope.calculatePriceOfEquipment(basket.equipment, roundNumber);
            equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : equipmentPrice * basket.amount;
            return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice) : '';
        };


        $scope.calculatePriceOfBasketProposal = (basket: Basket, roundNumber?: number, toDisplay?: boolean) => {
            let equipmentPrice =  $scope.calculatePriceOfEquipment(basket.equipment,2);
            equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : equipmentPrice * basket.amount;
            return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice) : '';
        };

        $scope.calculatePriceOfBasketUnity = (basket: Basket, roundNumber?: number, toDisplay?: boolean) => {
            let equipmentPrice = $scope.calculatePriceOfEquipment(basket.equipment, roundNumber);
            equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : equipmentPrice * 1;
            return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice ) : '';
        };

        $scope.priceDisplay = (basket: Basket) => {
            return $scope.calculatePriceOfBasketUnity(basket, 2, true);
        };

        $scope.displayLightboxDelete = (basket: Basket) => {
            //template.open('basket.delete', 'customer/campaign/basket/delete-confirmation');
            //$scope.basketToDelete = basket;
            //$scope.display.lightbox.deleteBasket = true;
            //Utils.safeApply($scope);
            $scope.deleteBasket(basket);
        };

        $scope.deleteBasket = async (basket: Basket) => {
            let { status } = await basket.delete();
            if (status === 200) {
                $scope.campaign.nb_panier -= 1;
                await $scope.notifyBasket('deleted', basket);
            }
            $scope.cancelBasketDelete();
            await $scope.baskets.sync($routeParams.idCampaign, $scope.current.structure.id);
            Utils.safeApply($scope);
        };

        $scope.cancelBasketDelete = () => {
            delete $scope.basketToDelete;
            $scope.display.lightbox.deleteBasket = false;
            template.close('basket.delete');
            Utils.safeApply($scope);
        };

        $scope.updateBasketAmount = (basket: Basket) => {
            if (basket.amount === 0) {
                $scope.displayLightboxDelete(basket);
            }
            else if (basket.amount > 0) {
                basket.updateAmount();
            }
        };

        $scope.updateBasketComment = async (basket: Basket) => {
            if (!basket.comment || basket.comment.trim() == "") {
                basket.comment = "";
            }
            await basket.updateComment();
            Utils.safeApply($scope);
        };

        $scope.takeClientOrder = async (basket_name: string) => {
            $scope.totalPriceOrder = $scope.calculatePriceOfEquipments($scope.baskets_test, 2);
            let {status, data} = await $scope.baskets_test.takeOrder(parseInt($routeParams.idCampaign), $scope.current.structure, basket_name);
            if(status === 200) {
                let nbr_equipment = $scope.baskets_test.all.length;
                $scope.confirmOrder(data, nbr_equipment)
                $scope.totalPrice = $scope.calculatePriceOfEquipments($scope.baskets_test, 2);
                await $scope.baskets_test.sync(parseInt($routeParams.idCampaign), $scope.current.structure.id);
                $scope.cancelConfirmBasketName();
            }
        };

        $scope.confirmOrder = (data, nbr_equipment) => {
            $scope.campaign.nb_order += 1;
            $scope.campaign.order_notification += 1;
            $scope.campaign.nb_order_waiting += nbr_equipment;
            $scope.campaign.nb_panier = 0;
            $scope.campaign.purse_amount = data.amount;
            let message = lang.translate('crre.confirm.order.message1') +
                "<strong>" +
                parseFloat($scope.totalPriceOrder).toLocaleString(undefined,{minimumFractionDigits: 2, maximumFractionDigits: 2}) +
                " " + idiom.translate('money.symbol') +
                "</strong>" +
                lang.translate('crre.confirm.order.message2');
            toasts.confirm(message);
            //template.open('basket.order', 'customer/campaign/basket/order-confirmation');
            //$scope.display.lightbox.confirmOrder = true;
            Utils.safeApply($scope);
        };

        $scope.confirmBasketName = () => {
            template.open('basket.name', 'customer/campaign/basket/basket-name-confirmation');
            $scope.display.lightbox.confirmbasketName = true;
            Utils.safeApply($scope);
        };

        $scope.cancelConfirmOrder = () => {
            $scope.display.lightbox.confirmOrder = false;
            template.close('basket.order');
            Utils.safeApply($scope);
        };

        $scope.cancelConfirmBasketName = () => {
            $scope.display.lightbox.confirmbasketName = false;
            template.close('basket.name');
            Utils.safeApply($scope);
        };


        $scope.validOrder = (baskets: Baskets) => {
            let equipmentsBasket = _.pluck(baskets.all, 'equipment' );
            return $scope.calculatePriceOfEquipments(baskets) <= $scope.campaign.purse_amount
                && _.findWhere( equipmentsBasket, {status : 'EPUISE'}) === undefined;
/*                &&  _.findWhere( equipmentsBasket, {status : 'UNAVAILABLE'}) === undefined;*/
        };

        $scope.checkPrice = async (baskets: Baskets) => {
            let priceIs0 = false;
            baskets.all.forEach(basket =>{
                if(basket.equipment.price === 0){
                    priceIs0 = true;
                }
            });
            if(priceIs0){
                toasts.warning("basket.price.null")
            }else{
                $scope.baskets_test = baskets;
                $scope.confirmBasketName();
            }
        };

    }]);