import {ng, idiom as lang} from 'entcore';
import {IDirective, IParseService, IScope} from "angular";
import {OrderClient, OrdersClient} from "../../model";
import {I18nUtils} from "../../utils/i18n.utils";

interface IViewModel extends ng.IController, IConfirmOrderValidatorProps {
    confirmOrder?();
    cancelOrder?();
    checkDuplicate(orders: OrdersClient): void;
    getQuantity(amount: number): string;

    comment: string;
    lang: typeof lang;

}

interface IConfirmOrderValidatorScope extends IScope, IConfirmOrderValidatorProps {
    vm: IViewModel;
}

interface IConfirmOrderValidatorProps {
    onConfirm?;
    onCancel?;
    ordersSelected: OrderClient[];
}

class Controller implements IViewModel {
    comment: string;
    lang: typeof lang;

    ordersSelected: OrderClient[];
    ordersDuplicated: OrderClient[];

    constructor(private $scope: IConfirmOrderValidatorScope) {
        this.ordersDuplicated = [];
    }

    $onInit() {
        this.checkDuplicate();
        this.lang = lang;
    }

    $onDestroy() {
    }

    checkDuplicate = (): void => {
        let seen = {};
        for (let i = 0; i < this.ordersSelected.length; i++) {
            let orderClient: OrderClient = this.ordersSelected[i];
            let key: string = orderClient.equipment_key + "-" + orderClient.amount;
            if (key in seen) {
                let foundOrderClient: OrderClient = seen[key];
                let foundIndex: number = this.ordersDuplicated.findIndex(order => order.equipment_key === foundOrderClient.equipment_key && order.amount === foundOrderClient.amount);
                if (foundIndex === -1) {
                    this.ordersDuplicated.push(foundOrderClient);
                }
                if (!this.ordersDuplicated.some(order => order.equipment_key === orderClient.equipment_key && order.amount === orderClient.amount)) {
                    this.ordersDuplicated.push(orderClient);
                }
            } else {
                seen[key] = orderClient;
            }
        }
    };

    getQuantity = (amount: number): string => {
        return I18nUtils.getWithParams("crre.confirm.basket.quantity", [
            amount.toString()
        ]);
    }


}

function directive($parse: IParseService): IDirective {
    return {
        restrict: 'E',
        templateUrl: `/crre/public/ts/directives/confirm-order-validator/confirm-order-validator.html`,
        scope: {
            display: '=',
            onConfirm: '&',
            onCancel: '&',
            ordersSelected : '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', '$parse', Controller],
        /* interaction DOM/element */
        link: function ($scope: IConfirmOrderValidatorScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
            vm.confirmOrder = () => {
                $parse($scope.vm.onConfirm())($scope.vm.comment);
            };
            vm.cancelOrder = () => {
                $parse($scope.vm.onCancel())({});
            };
        }
    }
}

export const confirmOrderValidator = ng.directive('confirmOrderValidator', directive);