import {ng, idiom as lang} from 'entcore';
import {IDirective, IParseService, IScope} from "angular";

interface IViewModel extends ng.IController, IConfirmOrderValidatorProps {
    confirmOrder?();
    cancelOrder?();
    comment: string;
    lang: typeof lang;
}

interface IConfirmOrderValidatorScope extends IScope, IConfirmOrderValidatorProps {
    vm: IViewModel;
}

interface IConfirmOrderValidatorProps {
    onConfirm?;
    onCancel?;
}

class Controller implements IViewModel {
    comment: string;
    lang: typeof lang;

    constructor(private $scope: IConfirmOrderValidatorScope) {
    }

    $onInit() {
        this.lang = lang;
    }

    $onDestroy() {
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