import {ng, template} from 'entcore';
import {Basket, Campaign, Equipment, Utils} from '../../../model';
import {TypeCatalogEnum} from "../../../enum/type-catalog-enum";

export const equipmentController = ng.controller('equipmentController',
    ['$scope', '$routeParams', '$anchorScroll', ($scope, $routeParams, $anchorScroll) => {
        $scope.scrollTo = function (id) {
            $anchorScroll(id);
        };

        $scope.validArticle = () => {
            return $scope.basket.amount > 0;
        };

        $scope.goBack = () => {
            history.back();
        };

        $scope.calculatePriceOfBasket = (basket: Basket, roundNumber: number, toDisplay?: boolean): string => {
            return Utils.calculatePriceOfBasket(basket, roundNumber, toDisplay);
        };

        $scope.calculatePriceOfEquipment = (equipment: Equipment, roundNumber: number): string => {
            return Utils.calculatePriceOfEquipment(equipment, roundNumber);
        };

        $scope.computeOffer = (): void => {
            $scope.offers = Utils.computeOffer($scope.basket, $scope.basket.equipment, $scope.offerStudent, $scope.offerTeacher);
        };

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
            array.forEach(function (item, index) {
                return_string += item.libelle;
                if (array.length - 1 != index) {
                    return_string += ", ";
                }
            });
            return return_string;
        };

        $scope.addBasketItem = async (basket: Basket, campaign?: Campaign, id_structure?: string) => {
            if (basket.id_campaign == undefined && campaign.accessible) {
                basket.id_campaign = campaign.id;
                basket.id_structure = id_structure;
                $scope.$emit('eventEmitedCampaign', campaign);
                $scope.campaign = campaign;
                $scope.display.lightbox.choosecampaign = false;
            }
            let {status} = await basket.create();
            if (status === 200 && basket.amount > 0) {
                if ($scope.campaign.nb_panier)
                    $scope.campaign.nb_panier += 1;
                else
                    $scope.campaign.nb_panier = 1;
                await $scope.notifyBasket('added', basket);
            }
            Utils.safeApply($scope);
        };

        $scope.amountIncrease = async () => {
            if($scope.basket.amount) {
                $scope.basket.amount += 1;
            } else {
                $scope.basket.amount = 1;
            }
            if ($scope.basket.equipment.typeCatalogue == TypeCatalogEnum.NUMERIC) {
                $scope.offers = await Utils.computeOffer($scope.basket, $scope.basket.equipment,
                    $scope.offerStudent, $scope.offerTeacher);
            }
            Utils.safeApply($scope);
        };

        $scope.amountDecrease = async () => {
            if ($scope.basket.amount > 1) {
                $scope.basket.amount -= 1;
                if ($scope.basket.equipment.typeCatalogue == TypeCatalogEnum.NUMERIC) {
                    $scope.offers = await Utils.computeOffer($scope.basket, $scope.basket.equipment,
                        $scope.offerStudent, $scope.offerTeacher);
                }
            }
            Utils.safeApply($scope);
        };

        $scope.isOffer = () => {
            return $scope.offerTeacher.length + $scope.offerStudent.length > 0;
        }

    }]);