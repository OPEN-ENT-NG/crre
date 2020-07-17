import {_, moment, ng, template, toasts} from 'entcore';
import {Basket, Baskets, Notification, Utils} from '../../model';

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

        $scope.isProposed = (basket: Basket) => {
            return (basket.price_proposal);
        }

        $scope.hasOneSelected = (baskets: Baskets) => {
            let hasSelected = false;
            baskets.all.map((basket) => {
                if (basket.selected) {
                    hasSelected = true;
                }
            })
            return hasSelected;
        }
        $scope.calculatePriceOfEquipments = (baskets: Baskets, roundNumber?: number) => {
            let totalPrice = 0;
            baskets.all.map((basket) => {
                if (basket.equipment.status !== 'AVAILABLE') return false;
                if (!$scope.hasOneSelected(baskets)) {
                    let basketItemPrice = $scope.calculatePriceOfBasket(basket, 2, false);
                    totalPrice += !isNaN(basketItemPrice) ? parseFloat(basketItemPrice) : 0;
                } else {
                    if (basket.selected) {
                        let basketItemPrice = $scope.calculatePriceOfBasket(basket, 2, false);
                        totalPrice += !isNaN(basketItemPrice) ? parseFloat(basketItemPrice) : 0;
                    }
                }
            });
            return (!isNaN(totalPrice)) ? (roundNumber ? totalPrice.toFixed(roundNumber) : totalPrice ) : '';
        };
        $scope.calculatePriceOfEquipmentsProposal = (baskets: Baskets, roundNumber?: number) => {
            let totalPrice = 0;
            baskets.all.map((basket) => {
                if (basket.equipment.status !== 'AVAILABLE') return false;
                if (!$scope.hasOneSelected(baskets)) {
                    let basketItemPrice = $scope.calculatePriceOfBasketProposal(basket, 2, false);
                    totalPrice += !isNaN(basketItemPrice) ? parseFloat(basketItemPrice) : 0;
                }

                else {
                    if (basket.selected) {
                        let basketItemPrice = $scope.calculatePriceOfBasketProposal(basket, 2, false);
                        totalPrice += !isNaN(basketItemPrice) ? parseFloat(basketItemPrice) : 0;
                    }
                }
            });
            return (!isNaN(totalPrice)) ? (roundNumber ? totalPrice.toFixed(roundNumber) : totalPrice) : '';
        };

        $scope.calculatePriceOfBasket = (basket: Basket, roundNumber?: number, toDisplay?: boolean) => {
            let equipmentPrice = $scope.calculatePriceOfEquipment(basket.equipment, true, roundNumber);
            equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : equipmentPrice * basket.amount;
            return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice) : '';
        };


        $scope.calculatePriceOfBasketProposal = (basket: Basket, roundNumber?: number, toDisplay?: boolean) => {
            let equipmentPrice =  $scope.calculatePriceOfEquipment(basket.equipment, true, roundNumber);
            if (basket.price_proposal === false || basket.price_proposal ===  null || basket.price_proposal === undefined) {
                equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : equipmentPrice * basket.amount;
            } else {
                equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : basket.price_proposal * basket.amount;
            }
            return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice) : '';
        };

        $scope.resetPriceProposal = (basket: Basket) => {
            //    basket.price_proposal = $scope.calculatePriceOfBasketUnity(basket,2);
            basket.price_proposal = null;
            basket.display_price_editable = false;
            Utils.safeApply($scope);
            basket.updatePriceProposal();

        }


        $scope.calculatePriceOfBasketUnity = (basket: Basket, roundNumber?: number, toDisplay?: boolean) => {
            let equipmentPrice = $scope.calculatePriceOfEquipment(basket.equipment, true, roundNumber);
            equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : equipmentPrice * 1;
            return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice ) : '';
        };

        $scope.priceDisplay = (basket: Basket) => {
            if (basket.price_proposal === false || basket.price_proposal ===  null || basket.price_proposal === undefined) {
                return $scope.calculatePriceOfBasketUnity(basket, 2, true);
            } else {
                return basket.price_proposal;
            }
        }

        $scope.displayPriceEdition = (basket: Basket) => {

            basket.display_price_editable = true;
            Utils.safeApply($scope);
        }


        $scope.calculeDeliveryDate = () => {
            return moment().add(60, 'days').calendar();
        };


        $scope.displayOptions = (index: number) => {
            $scope.display.equipmentOption[index] = !$scope.display.equipmentOption[index] ;
            Utils.safeApply($scope);
        };

        $scope.displayLightboxDelete = (basket: Basket) => {
            template.open('basket.delete', 'customer/campaign/basket/delete-confirmation');
            $scope.basketToDelete = basket;
            $scope.display.lightbox.deleteBasket = true;
            Utils.safeApply($scope);
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
        }

        $scope.updateBasketPriceProposal = (basket: Basket) => {
            basket.updatePriceProposal();
            console.log(basket)
            basket.display_price_editable = false;
            Utils.safeApply($scope);
        }

        $scope.takeClientOrder = async (baskets: Baskets, idProject: number) => {
            $scope.totalPriceOrder = $scope.calculatePriceOfEquipments(baskets, 2);
            let {status, data} = await baskets.takeOrder(parseInt($routeParams.idCampaign), $scope.current.structure, idProject);
            $scope.totalPrice = $scope.calculatePriceOfEquipments(baskets, 2);

            //   $scope.totalPriceProposal = $scope.calculatePriceOfEquipmentsProposal(baskets, 2)
            await baskets.sync(parseInt($routeParams.idCampaign), $scope.current.structure.id);
            status === 200 ?  $scope.confirmOrder(data) :  null ;
        };
        $scope.confirmOrder = (data) => {
            $scope.campaign.nb_panier = $scope.baskets.all.length;
            $scope.campaign.nb_order = data.nb_order;
            $scope.campaign.purse_amount = data.amount;
            template.open('basket.order', 'customer/campaign/basket/order-confirmation');
            $scope.display.lightbox.confirmOrder = true;
            Utils.safeApply($scope);
        };
        $scope.cancelConfirmOrder = () => {
            $scope.display.lightbox.confirmOrder = false;
            template.close('basket.order');
            Utils.safeApply($scope);
        };
        $scope.validOrder = (baskets: Baskets) => {
            let equipmentsBasket = _.pluck(baskets.all, 'equipment' );
            return $scope.calculatePriceOfEquipments(baskets) <= $scope.campaign.purse_amount
                && _.findWhere( equipmentsBasket, {status : 'OUT_OF_STOCK'}) === undefined
                &&  _.findWhere( equipmentsBasket, {status : 'UNAVAILABLE'}) === undefined;
        };
        $scope.takeClientProject = async (baskets: Baskets) => {
            let priceIs0 = false;
            baskets.all.forEach(basket =>{
                console.log(basket)
                if(basket.price_proposal === null && basket.equipment.price === 0 || basket.price_proposal === 0 ){
                    priceIs0 = true;
                }
            })
            if(priceIs0){
                toasts.warning("basket.price.null")
            }else{
                template.open('basket.project', 'customer/campaign/basket/project-confirmation');
                $scope.display.lightbox.createProject = true;
            }


        }
        $scope.cancelProjectCreate = () => {
            $scope.display.lightbox.createProject = false;
            template.close('basket.project');
            Utils.safeApply($scope);
        }


        $scope.openAddDocumentsLightbox = (basket: Basket) => {
            $scope.basket = basket;
            $scope.files = [];
            $scope.display.lightbox.addDocuments = true;
            Utils.safeApply($scope);
        };

        $scope.endUpload = (files) => {
            $scope.basket.files = $scope.basket.files || [];
            for (let i = 0; i < files.length; i++) {
                $scope.basket.files.push(files[i]);
            }
            $scope.display.lightbox.addDocuments = false;
            Utils.safeApply($scope);
        };

        $scope.deleteBasketDocument = async (basket: Basket, file) => {
            try {
                file.status = 'loading';
                Utils.safeApply($scope);
                await basket.deleteDocument(file);
                basket.files = _.reject(basket.files, (doc) => doc.id === file.id);
                toasts.confirm('lystore.basket.file.delete.success');
            } catch (err) {
                toasts.warning('lystore.basket.file.delete.error');
                delete file.status;
            } finally {
                Utils.safeApply($scope);
            }
        };

    }]);