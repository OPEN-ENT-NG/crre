/**
 * Created by rahnir on 22/01/2018.
 */
/**
 * Created by rahnir on 18/01/2018.
 */
import {_, ng, template} from 'entcore';
import {Basket, Campaign, Equipment, Utils} from '../../model';
import forEach = require("core-js/fn/array/for-each");


export const catalogController = ng.controller('catalogController',
    ['$scope', '$routeParams', ($scope) => {
        this.init = () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.alloptionsSelected = false;
            $scope.equipment = new Equipment();
            $scope.subjects = [];
            $scope.initPopUpFilters();
        };

        $scope.addFilter = async (event) => {
            $scope.word = event.target.value;
            $scope.nbItemsDisplay = $scope.pageSize;
            await $scope.equipments.getFilterEquipments(event.target.value);
            $scope.$apply();
        };

        $scope.getFilter = async (word: string, filter: string) => {
            $scope.nbItemsDisplay = $scope.pageSize;
            await $scope.equipments.getFilterEquipments(word, filter);
            $scope.$apply();
        };

        $scope.validArticle = () => {
            return $scope.basket.amount > 0;
        };
        $scope.switchAll = (model: boolean, collection) => {
            collection.forEach((col) => {col.selected = col.required ? false : col.selected = model; });
            Utils.safeApply($scope);
        };
        $scope.thereAreOptionalOptions = (equipment: Equipment) => {
            return !(_.findWhere(equipment.options, {required : false}) === undefined) ;
        };
        $scope.chooseCampaign = async () => {
            await $scope.initStructures();
            await $scope.initCampaign($scope.current.structure);
            template.open('campaign.name', 'customer/campaign/basket/campaign-name-confirmation');
            $scope.display.lightbox.choosecampaign = true;
            Utils.safeApply($scope);
        };
        $scope.cancelChooseCampaign = () => {
            $scope.display.lightbox.choosecampaign = false;
            template.close('campaign.name');
            Utils.safeApply($scope);
        };

        $scope.setCampaignId = (campaign: Campaign) => {
          $scope.campaign = campaign;
        }

        $scope.formatGrade = (grades: any[]) => {
            let grade_string = "";
            grades.forEach(function(grade, index) {
               grade_string += grade.libelle;
               if(grades.length - 1 != index) {
                   grade_string += ", ";
               }
            });
            return grade_string;
        }

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
        $scope.amountIncrease = () => {
            $scope.basket.amount += 1;
        };
        $scope.amountDecrease = () => {
            if($scope.basket.amount)
                $scope.basket.amount -= 1;
        };

        $scope.getFilters = () => {
           $scope.equipments.getFilters();
        };

        $scope.durationFormat = (nbr : number) => {
            if(nbr == 0)
                return "Illimitée";
            else if(nbr == 1)
                return nbr.toString() + " année scolaire";
            else
                return nbr.toString() + " années scolaires";
        };

        $scope.initPopUpFilters = (filter?:string) => {
            let value = $scope.$eval(filter);

            $scope.showPopUpColumnsGrade = $scope.showPopUpColumnsEditor = $scope.showPopUpColumnsSubject =
                $scope.showPopUpColumnsOS = $scope.showPopUpColumnsDocumentsTypes = $scope.showPopUpColumnsPublic = false;

            if (!value) {
                switch (filter) {
                    case 'showPopUpColumnsSubject': $scope.showPopUpColumnsSubject = true; break;
                    case 'showPopUpColumnsPublic': $scope.showPopUpColumnsPublic = true; break;
                    case 'showPopUpColumnsGrade': $scope.showPopUpColumnsGrade = true; break;
                    case 'showPopUpColumnsDocumentsTypes': $scope.showPopUpColumnsDocumentsTypes = true; break;
                    case 'showPopUpColumnsEditor': $scope.showPopUpColumnsEditor = true; break;
                    case 'showPopUpColumnsOS': $scope.showPopUpColumnsOS = true; break;
                    default: break;
                }
            }

        };

        this.init();
    }]);