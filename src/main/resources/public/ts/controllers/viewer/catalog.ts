/**
 * Created by rahnir on 22/01/2018.
 */
/**
 * Created by rahnir on 18/01/2018.
 */
import {_, ng, template} from 'entcore';
import {Basket, Campaign, Equipment, Utils} from '../../model';


export const catalogController = ng.controller('catalogController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        this.init = () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.alloptionsSelected = false;
            $scope.equipment = new Equipment();
            /*        $scope.subjects = [];*/
            $scope.initPopUpFilters();
        };

        $scope.addFilter = (event) => {
            //$scope.equipments.sort.filters.push(event.target.value);
            $scope.word = event.target.value;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.getFilterEquipments(event.target.value);
            $scope.$apply();
        };

        $scope.getFilter = (word: string, filter: string) => {
            //$scope.equipments.sort.filters.push(event.target.value);
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.getFilterEquipments(word, filter);
            $scope.$apply();
        };

/*        $scope.addfilterWords = (filterWrod) => {
            if (filterWrod !== '') {
                $scope.search.filterWrods = _.union($scope.search.filterWrods, [filterWrod]);
                $scope.search.filterWrod = '';
                Utils.safeApply($scope);
            }
        };

        $scope.dropEquipmentFilter = (filter: string) => {
            $scope.equipments.sort.filters = _.without($scope.equipments.sort.filters, filter);
            $scope.equipments.sync();
        };*/

        $scope.openEquipment = (equipment: Equipment) => {
            if (equipment.status === 'AVAILABLE') {
                $scope.redirectTo(`/equipments/catalog/equipment/${equipment.id}`);
                $scope.display.equipment = true;
            }
        };
        $scope.validArticle = (equipment: Equipment) => {
            return !isNaN(parseFloat($scope.calculatePriceOfEquipment(equipment)))
                && $scope.basket.amount > 0;
        };
        $scope.switchAll = (model: boolean, collection) => {
            collection.forEach((col) => {col.selected = col.required ? false : col.selected = model; });
            Utils.safeApply($scope);
        };
        $scope.thereAreOptionalOptions = (equipment: Equipment) => {
            return !(_.findWhere(equipment.options, {required : false}) === undefined) ;
        };
        $scope.chooseCampaign = async (basket: Basket) => {
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
                $scope.showPopUpColumnsOS = $scope.showPopUpColumnsDocumentsTypes = $scope.showPopUpColumnsDiplomes = false;

            if (!value) {
                switch (filter) {
                    case 'showPopUpColumnsSubject': $scope.showPopUpColumnsSubject = true; break;
                    case 'showPopUpColumnsDiplomes': $scope.showPopUpColumnsDiplomes = true; break;
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