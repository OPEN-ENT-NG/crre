import {ng} from "entcore";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {Campaign, OrderClient, OrdersClient} from "../../model";
import {CREDIT_TYPE_ENUM} from "../../enum/credit-type-enum";
import {OrderSearchAmount} from "../../model/OrderSearchAmount";

interface IViewModel extends ng.IController, IOrderWaitingPriceDisplayProps {
    totalSelectedItems(): number;
    totalSelectedPriceAll(): number;
    totalSelectedPriceConsumable(): number;
    totalSelectedPrice(): number;
}

interface IOrderWaitingPriceDisplayProps {
    campaign: Campaign
    ordersClient: OrdersClient
    amountTotal: OrderSearchAmount
    allOrdersSelected: OrdersClient
}

interface IOrderWaitingPriceDisplayScope extends IScope, IOrderWaitingPriceDisplayProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    campaign: Campaign
    ordersClient: OrdersClient
    amountTotal: OrderSearchAmount
    allOrdersSelected: OrdersClient

    constructor(private $scope: IOrderWaitingPriceDisplayScope,
                private $location: ILocationService,
                private $window: IWindowService,
                private $parse: IParseService) {
    }

    $onInit() {
    }

    $onDestroy() {
    }

    totalSelectedItems(): number {
        if (this.allOrdersSelected) {
            return this.amountTotal.total;
        }

        return this.ordersClient.all
            .filter((orderClient: OrderClient) => orderClient.selected && orderClient.amount > 0)
            .map((orderClient: OrderClient) => orderClient.amount)
            .reduce((partialSum, a) => partialSum + a, 0);
    }

    totalSelectedPriceAll(): number {
        if (this.allOrdersSelected) {
            return this.amountTotal.total_filtered + this.amountTotal.total_filtered_consumable;
        }

        return this.ordersClient.all
            .filter((orderClient: OrderClient) => orderClient.selected)
            .map((orderClient: OrderClient) => orderClient.amount * orderClient.price)
            .reduce((partialSum, a) => partialSum + a, 0);
    }

    totalSelectedPriceConsumable(): number {
        if (this.allOrdersSelected) {
            return this.amountTotal.total_filtered_consumable;
        }

        return this.ordersClient.all
            .filter((orderClient: OrderClient) => orderClient.selected)
            .filter((orderClient: OrderClient) => orderClient.campaign.use_credit == CREDIT_TYPE_ENUM.CONSUMABLE_CREDITS)
            .map((orderClient: OrderClient) => orderClient.amount * orderClient.price)
            .reduce((partialSum, a) => partialSum + a, 0);
    }

    totalSelectedPrice(): number {
        if (this.allOrdersSelected) {
            return this.amountTotal.total_filtered;
        }

        return this.ordersClient.all
            .filter((orderClient: OrderClient) => orderClient.selected)
            .filter((orderClient: OrderClient) => orderClient.campaign.use_credit == CREDIT_TYPE_ENUM.CREDITS)
            .map((orderClient: OrderClient) => orderClient.amount * orderClient.price)
            .reduce((partialSum, a) => partialSum + a, 0);
    }
}

function directive($parse: IParseService) {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/order-waiting-price-display/order-waiting-price-display.html`,
        scope: {
            campaign: '=',
            ordersClient: '=',
            amountTotal: '=',
            allOrdersSelected: "=",
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', '$parse', Controller],
        /* interaction DOM/element */
        link: function ($scope: IOrderWaitingPriceDisplayScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {

        }
    }
}

export const orderWaitingPriceDisplay = ng.directive('orderWaitingPriceDisplay', directive)