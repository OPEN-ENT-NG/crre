import {Behaviours, ng, template, toasts} from 'entcore';
import {IDirective, IScope} from "angular";
import {AxiosError} from "axios";
import {OrderRegion, OrdersRegion, Projects, Utils} from "../../model";

interface IViewModel extends ng.IController, ICrreProps {
    generateLibraryOrder(): Promise<void>;

    checkRejectedOrder(): boolean;

    // link method
    closeWaitingAdminLightbox?(): void;
}

interface ICrreScope extends IScope, ICrreProps {
    vm: IViewModel;
}

interface ICrreProps {
    display?: any;
    projects?: Projects;

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
        if (this.$scope.vm.display.allOrdersSelected || !this.$scope.vm.projects.hasSelectedOrders()) {
            return this.$scope.vm.projects.extractAllOrders();
        } else {
            return this.$scope.vm.projects.extractSelectedOrders();
        }
    }

    generateLibraryOrder = async () : Promise<void> => {
        const selectedOrders : OrdersRegion = this.extractSelectedOrders();
        this.$scope.vm.projects.all = [];
        this.$scope.vm.display.loading = true;
        this.$scope.vm.display.lightbox.waitingAdmin = false;
        template.close('lightbox.waitingAdmin');
        Utils.safeApply(this.$scope);
        await selectedOrders.generateLibraryOrder().then(() => {
            toasts.confirm('crre.order.region.library.create.message');
            this.$scope.vm.display.toggle = false;
            Utils.safeApply(this.$scope);
            this.$scope.vm.display.allOrdersSelected = false;
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

    checkRejectedOrder = () : boolean => {
        return this.extractSelectedOrders().all.filter((order : OrderRegion) => order.status === 'REJECTED').length > 0;
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
            projects: '=',
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