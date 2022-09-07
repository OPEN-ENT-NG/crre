import {ng, template} from 'entcore';
import {Basket, Campaign, Utils} from '../../../model';

export const equipmentController = ng.controller('equipmentController',
    ['$scope', '$routeParams', '$anchorScroll', ($scope, $routeParams, $anchorScroll) => {
        $scope.scrollTo = function(id) {
            $anchorScroll(id);
        };

        $scope.validArticle = () => {
            return $scope.basket.amount > 0;
        };

        $scope.goBack = () => {
            history.back();
        }

        $scope.chooseCampaign = async () => {
            await $scope.initStructures();
            await $scope.initCampaign($scope.current.structure);
            await template.open('campaign.name', 'prescriptor/basket/campaign-name-confirmation');
            $scope.display.lightbox.choosecampaign = true;
            Utils.safeApply($scope);
        };

        $scope.cancelChooseCampaign = () => {
            $scope.display.lightbox.choosecampaign = false;
            template.close('campaign.name');
            Utils.safeApply($scope);
        };

        $scope.formatMultiple = (array: any[]) => {
            let return_string = "";
            array.forEach(function(item, index) {
                return_string += item.libelle;
               if(array.length - 1 != index) {
                   return_string += ", ";
               }
            });
            return return_string;
        };

        $scope.addBasketItem = async (basket: Basket, campaign?: Campaign, id_structure?: string) => {
            if(basket.id_campaign === undefined && campaign.accessible) {
                basket.id_campaign = campaign.id;
                basket.id_structure= id_structure;
                $scope.$emit('eventEmitedCampaign', campaign);
                $scope.campaign = campaign;
                $scope.display.lightbox.choosecampaign = false;
            }
            let { status } = await basket.create();
            if (status === 200 && basket.amount > 0 ) {
                if( $scope.campaign.nb_panier)
                    $scope.campaign.nb_panier += 1;
                else
                    $scope.campaign.nb_panier = 1;
                await $scope.notifyBasket('added', basket);
            }
            Utils.safeApply($scope);
        };

        $scope.amountIncrease = async () => {
            $scope.basket.amount += 1;
            if ($scope.basket.equipment.type === 'articlenumerique') {
                await $scope.computeOffer();
            }
        };

        $scope.amountDecrease = async () => {
            if ($scope.basket.amount)
                $scope.basket.amount -= 1;
            if ($scope.basket.equipment.type === 'articlenumerique') {
                await $scope.computeOffer();
            }
        };

        $scope.isOffer = () => {
            return $scope.offerTeacher.length + $scope.offerStudent.length > 0;
        }

    }]);