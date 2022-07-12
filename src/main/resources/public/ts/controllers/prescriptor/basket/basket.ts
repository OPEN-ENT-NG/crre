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

        $scope.calculatePriceOfEquipments = (baskets: Baskets, roundNumber?: number) => {
            let totalPrice = 0;
            baskets.all.map((basket) => {
                if (!Utils.isAvailable(basket.equipment))
                    return false;
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
                if (basket.equipment.disponibilite[0].valeur !== 'DISPONIBLE'  && basket.equipment.disponibilite[0].valeur !== 'PRECOMMANDE') return false;
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

        $scope.priceDisplay = (basket: Basket) => {
            let equipmentPrice = parseFloat(String($scope.calculatePriceOfEquipment(basket.equipment, 2)));
            return (!isNaN(equipmentPrice)) ? (2 ? equipmentPrice.toFixed(2) : equipmentPrice ) : '';
        };

        $scope.deleteBasket = async (basket: Basket) => {
            let { status } = await basket.delete();
            if (status === 200) {
                $scope.campaign.nb_panier -= 1;
                await $scope.notifyBasket('deleted', basket);
            }
            await $scope.baskets.sync($routeParams.idCampaign, $scope.current.structure.id, $scope.campaign.reassort);
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