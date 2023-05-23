import {Behaviours, ng, template, toasts} from 'entcore';
import {IDirective, IScope} from "angular";
import {AxiosError} from "axios";
import {OrderRegion, OrdersRegion, Utils} from "../../model";
import {ORDER_STATUS_ENUM} from "../../enum/order-status-enum";

interface IViewModel extends ng.IController, ICrreProps {
    generateLibraryOrder(): Promise<void>;

    checkStatusOrder(status : string): boolean;
    checkHistoricalStatusOrder(): boolean;
    checkRejectedStatusOrder(): boolean;

    // link method
    closeWaitingAdminLightbox?(): void;
}

interface ICrreScope extends IScope, ICrreProps {
    vm: IViewModel;
}

interface ICrreProps {
    display?: any;

    onCloseWaitingAdminLightbox? : () => void;
}

class Controller implements IViewModel {

    constructor(private $scope: ICrreScope) {

    }

    $onInit() {
    }

    $onDestroy() {
    }

    extractSelectedOrders() : OrdersRegion {
        if (this.$scope.vm.display.allOrdersSelected || !this.$scope.vm.display.projects.hasSelectedOrders()) {
            return this.$scope.vm.display.projects.extractAllOrders();
        } else {
            return this.$scope.vm.display.projects.extractSelectedOrders();
        }
    }

    generateLibraryOrder = async () : Promise<void> => {
        this.$scope.vm.display.loading = true;
        this.$scope.vm.display.lightbox.waitingAdmin = false;
        template.close('lightbox.waitingAdmin');
        const selectedOrders : OrdersRegion = this.extractSelectedOrders();
        this.$scope.vm.display.projects.all = [];
        Utils.safeApply(this.$scope);
        await selectedOrders.generateLibraryOrder().then(() => {
            toasts.confirm('crre.order.region.library.create.message');
            this.$scope.vm.display.toggle = this.$scope.vm.display.allOrdersSelected = false;
            Utils.safeApply(this.$scope);
            Behaviours.applicationsBehaviours['crre'].SnipletScrollService
                .sendScroll(true);
        }).catch((error: AxiosError) => {
            if (error.response.status == 401){
                toasts.warning('crre.order.error.purse');
            } else {
                toasts.warning('crre.order.region.library.create.err');
            }
            this.$scope.vm.display.allOrdersSelected = false;
            Behaviours.applicationsBehaviours['crre'].SnipletScrollService
                .sendScroll(true);
        });

    };

    checkStatusOrder = (status : string) : boolean => {
        return this.extractSelectedOrders().all.filter((order : OrderRegion) => order.status === status).length > 0;
    };

    checkHistoricalStatusOrder = () : boolean => {
        return Utils.getHistoricalStatus().some((value: ORDER_STATUS_ENUM) => this.checkStatusOrder(value));
    };

    checkRejectedStatusOrder = () : boolean => {
        return this.checkStatusOrder(ORDER_STATUS_ENUM.REJECTED);
    };

    closeWaitingAdminLightbox = (): void => {
        this.$scope.vm.onCloseWaitingAdminLightbox();
    }
}

function directive(): IDirective {
    return {
        replace: true,
        restrict: 'E',
        templateUrl: `/crre/public/ts/directives/confirm-generate-library/confirm-generate-library.html`,
        scope: {
            display: '=',
            onCloseWaitingAdminLightbox: '&'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', Controller],
        /* interaction DOM/element */
        link: function (scope: ICrreScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const confirmGenerateLibrary = ng.directive('confirmGenerateLibrary', directive);