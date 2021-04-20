import {_, Behaviours, model, ng, template, toasts} from 'entcore';
import {Basket, Baskets, Utils} from '../../../model';

export const basketController = ng.controller('basketController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.display = {
            lightbox : {
                confirmbasketName: false,
            },
        };

        const hasOneSelected = (baskets: Baskets) => {
            let hasSelected = false;
            baskets.all.map((basket) => {
                if (basket.selected) { hasSelected = true; }
            });
            return hasSelected;
        };

        /**
         * Calculate the price of an equipment
         * @param {Equipment} equipment
         * @param {number} roundNumber [number of digits after the decimal point]
         * @returns {number}
         */
        const calculatePriceOfEquipment = (equipment: any, roundNumber?: number) => {
            let price = parseFloat(String(Utils.calculatePriceTTC(equipment, roundNumber)));
            return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price) : price;
        };

        $scope.calculatePriceOfEquipments = (baskets: Baskets, roundNumber?: number) => {
            let totalPrice = 0;
            baskets.all.map((basket) => {
                if (basket.equipment.disponibilite[0].valeur !== 'DISPONIBLE') return false;
                if (!hasOneSelected(baskets) || basket.selected) {
                    let basketItemPrice = $scope.calculatePriceOfBasket(basket,2);
                    totalPrice += !isNaN(basketItemPrice) ? parseFloat(basketItemPrice) : 0;
                }
            });
            return (!isNaN(totalPrice)) ? (roundNumber ? totalPrice.toFixed(roundNumber) : totalPrice ) : '';
        };

        $scope.calculateQuantity = (baskets: Baskets,numberOfEquipments:boolean) => {
            let quantity = 0;
            baskets.all.map((basket) => {
                if (basket.equipment.disponibilite[0].valeur !== 'DISPONIBLE') return false;
                if (!hasOneSelected(baskets) || basket.selected) {
                    if(numberOfEquipments){
                        quantity ++;
                    }else {
                        quantity += basket.amount;
                    }
                }
            });
            return quantity;
        };

        $scope.calculatePriceOfBasket = (basket: Basket, roundNumber?: number, toDisplay?: boolean) => {
            let equipmentPrice = parseFloat(String(calculatePriceOfEquipment(basket.equipment, roundNumber)));
            equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : equipmentPrice * basket.amount;
            return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice) : '';
        };

        $scope.priceDisplay = (basket: Basket) => {
            let equipmentPrice = parseFloat(String(calculatePriceOfEquipment(basket.equipment, 2)));
            return (!isNaN(equipmentPrice)) ? (2 ? equipmentPrice.toFixed(2) : equipmentPrice ) : '';
        };

        $scope.deleteBasket = async (basket: Basket) => {
            let { status } = await basket.delete();
            if (status === 200) {
                $scope.campaign.nb_panier -= 1;
                await $scope.notifyBasket('deleted', basket);
            }
            await $scope.baskets.sync($routeParams.idCampaign, $scope.current.structure.id);
            Utils.safeApply($scope);
        };

        $scope.updateBasketComment = async (basket: Basket) => {
            if (!basket.comment || basket.comment.trim() == "") {
                basket.comment = "";
            }
            await basket.updateComment();
            Utils.safeApply($scope);
        };

        const confirmBasketName = () => {
            template.open('basket.name', 'prescriptor/basket/basket-name-confirmation');
            $scope.display.lightbox.confirmbasketName = true;
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
                confirmBasketName();
            }
        };

        $scope.canUpdateReassort = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.reassort);
        };

    }]);