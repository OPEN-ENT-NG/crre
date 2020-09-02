/**
 * Created by rahnir on 22/01/2018.
 */
/**
 * Created by rahnir on 18/01/2018.
 */
import {_, ng} from 'entcore';
import {Basket, Equipment, Utils} from '../../model';


export const catalogController = ng.controller('catalogController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.nbNewItems = 10;
        $scope.alloptionsSelected = false;
        $scope.equipment = new Equipment();
        $scope.subjects = [];
        $scope.showPopUpColumnsGrade = $scope.showPopUpColumnsEditor = $scope.showPopUpColumnsSubject =
            $scope.showPopUpColumnsOS = $scope.showPopUpColumnsDocumentsTypes = $scope.showPopUpColumnsDiplomes = false;
        $scope.addFilter = (event) => {
                //$scope.equipments.sort.filters.push(event.target.value);
                $scope.equipments.getSearchEquipment(event.target.value);
                $scope.equipments.page = 0;
                $scope.$apply();
        };

        $scope.getFilter = (filter: string, filter_word: string) => {
            //$scope.equipments.sort.filters.push(event.target.value);
            $scope.equipments.getFilterEquipment(filter, filter_word);
            $scope.equipments.page = 0;
            $scope.$apply();
        };




        $scope.addfilterWords = (filterWrod) => {
            if (filterWrod !== '') {
                $scope.search.filterWrods = _.union($scope.search.filterWrods, [filterWrod]);
                $scope.search.filterWrod = '';
                Utils.safeApply($scope);
            }
        };

        $scope.dropEquipmentFilter = (filter: string) => {
            $scope.equipments.sort.filters = _.without($scope.equipments.sort.filters, filter);
            $scope.equipments.sync();
        };

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
        $scope.addBasketItem = async (basket: Basket) => {
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
            $scope.basket.amount -= 1;
        };

        $scope.getFilters = () => {
           $scope.equipments.getFilters();
        };

        $scope.durationFormat = (nbr : number) =>  {
            if(nbr == 0)
                return "Illimitée";
            else if(nbr == 1)
                return nbr.toString() + " année scolaire";
            else
                return nbr.toString() + " années scolaires";
        };
        $scope.onBottomScroll = () => {
            console.log("at the bottom");
            $scope.scrollHeight = window.scrollY;
            $scope.display.equipment = false;
            $scope.equipments.scrollPage();
            Utils.safeApply($scope);
            window.scrollTo(0,$scope.scrollHeight);
        };
    }]);