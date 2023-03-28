import {_, Behaviours, model, ng, template, toasts} from 'entcore';
import {Basket, Baskets, Utils} from '../../../model';

export const basketController = ng.controller('basketController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.display = {
            lightbox : {
                confirmbasketName: false,
            },
        };

        $scope.calculatePriceOfEquipments = (baskets :  Baskets, roundNumber : number) : string => {
            return Utils.calculatePriceOfEquipments(baskets,roundNumber);
        };

        $scope.calculatePriceOfBasket = (basket : Basket , roundNumber : number, toDisplay? : boolean) : string => {
            return Utils.calculatePriceOfBasket(basket , roundNumber, toDisplay);
        };

        $scope.calculateQuantity = (baskets : Baskets, numberOfEquipments : boolean) : number => {
            let quantity = 0;
            baskets.all.map((basket) => {
                if (Utils.isAvailable(basket.equipment) && (!Utils.hasOneSelected(baskets) || basket.selected)) {
                    if(numberOfEquipments){
                        quantity ++;
                    }else {
                        quantity += basket.amount;
                    }
                }
            });
            return quantity;
        };

        $scope.priceDisplay = (basket: Basket) : string => {
            let equipmentPrice : number = parseFloat(Utils.calculatePriceOfEquipment(basket.equipment, 2));
            return (!isNaN(equipmentPrice)) ? (2 ? equipmentPrice.toFixed(2) : equipmentPrice.toString() ) : '0';
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
                $scope.duplicateBaskets = checkDuplicate(baskets);
                confirmBasketName();
            }
        };

        const checkDuplicate = (baskets: Baskets): Basket[] => {
            let duplicateBaskets: Basket[] = [];
            let allBaskets: Basket[];
            let seen = {};
            allBaskets = baskets.selected.length > 0 ? baskets.selected : baskets.all;

            for (let i = 0; i < allBaskets.length; i++) {
                let basket: Basket = allBaskets[i];
                let key: string = basket.equipment.ean + "-" + basket.amount;
                if (key in seen) {
                    let foundBasket: Basket = seen[key];
                    let foundIndex: number = duplicateBaskets.findIndex((b: Basket) => b.equipment.ean === foundBasket.equipment.ean && b.amount === foundBasket.amount);
                    if (foundIndex === -1) {
                        duplicateBaskets.push(foundBasket);
                    }
                    if (!duplicateBaskets.some((b: Basket) => b.equipment.ean === basket.equipment.ean && b.amount === basket.amount)) {
                        duplicateBaskets.push(basket);
                    }
                } else {
                    seen[key] = basket;
                }
            }
            return duplicateBaskets;
        };

        $scope.canUpdateReassort = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.reassort);
        };

    }]);