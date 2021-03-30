import {_, ng, template} from 'entcore';
import {Basket, Campaign, Equipment, Filter, Filters, Offer, Offers, Utils} from '../../model';

export const catalogController = ng.controller('catalogController',
    ['$scope', '$routeParams', ($scope) => {
        this.init = () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipment = new Equipment();
            $scope.loading = true;
            $scope.labels = ["technologie", "dispositifDYS", "webAdaptatif", "exercicesInteractifs", "availableViaENT",
                "availableViaGAR", "canUseOffline", "needFlash", "corrigesPourEnseignants"];
            $scope.catalog = {
                subjects : [],
                public : [],
                grades : [],
                docsType : [],
                editors : []
            };
            $scope.correlationFilterES = {
                keys : ["subjects","public","grades","docsType","editors"],
                subjects : 'disciplines.libelle',
                public : 'publiccible',
                grades : 'niveaux.libelle',
                docsType : '_index',
                editors : 'editeur'
            };

            if($scope.isAdministrator()){
                $scope.goBackUrl = "crre#/equipments/catalog";
            }else if($scope.hasAccess() && !$scope.isValidator() && !$scope.isPrescriptor()){
                $scope.goBackUrl = "crre#/equipments/catalog/0";
            }else{
                $scope.goBackUrl = "crre#/equipments/catalog/" + $scope.campaign.id;
            }
        };

        $scope.addFilter = async () => {
            $scope.query.word = $scope.queryWord;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
            Utils.safeApply($scope);
        };

        $scope.getFilter = async () => {
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            $scope.filters = new Filters();
            for (const key of Object.keys($scope.catalog)) {
                $scope.catalog[key].forEach(item => {
                    let newFilter = new Filter();
                    newFilter.name = $scope.correlationFilterES[key];
                    newFilter.value = item.name;
                    $scope.filters.all.push(newFilter);
                })
            }
            if($scope.filters.all.length > 0) {
                await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
                Utils.safeApply($scope);
            } else {
                await $scope.equipments.getFilterEquipments($scope.query.word);
                Utils.safeApply($scope);
            }
        };

        $scope.dropElement = (item,key): void => {
            $scope.catalog[key] = _.without($scope.catalog[key], item);
            $scope.getFilter();
        };

        $scope.validArticle = () => {
            return $scope.basket.amount > 0;
        };

        $scope.computeOffer = () => {
            let amount = $scope.basket.amount;
            let gratuit = 0;
            let gratuite = 0;
            let offre = null;
            $scope.offers = new Offers();
            $scope.basket.equipment.offres[0].leps.forEach(function (offer) {
                offre = new Offer();
                offre.name = offer.licence[0].valeur;
                if(offer.conditions.length > 1) {
                    offer.conditions.forEach(function (condition) {
                        if(amount >= condition.conditionGratuite && gratuit < condition.conditionGratuite) {
                            gratuit = condition.conditionGratuite;
                            gratuite = condition.gratuite;
                        }
                    });
                } else {
                    gratuit = offer.conditions[0].conditionGratuite;
                    gratuite = offer.conditions[0].gratuite * Math.floor(amount/gratuit);
                }
                offre.value = gratuite;
                $scope.offers.all.push(offre);
            });
            return $scope.offers;
        };


        $scope.switchAll = (model: boolean, collection) => {
            collection.forEach((col) => {col.selected = col.required ? false : col.selected = model; });
            Utils.safeApply($scope);
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
            if($scope.basket.equipment.type === 'articlenumerique') {
                $scope.computeOffer();
            }
        };
        $scope.amountDecrease = () => {
            if($scope.basket.amount)
                $scope.basket.amount -= 1;
            if($scope.basket.equipment.type === 'articlenumerique') {
                $scope.computeOffer();
            }
        };

        $scope.durationFormat = (nbr : number) => {
            if(nbr == 0)
                return "Illimitée";
            else if(nbr == 1)
                return nbr.toString() + " année scolaire";
            else
                return nbr.toString() + " années scolaires";
        };
        this.init();
    }]);