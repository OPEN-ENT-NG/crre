import {_, moment, ng, idiom as lang} from "entcore";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {UserModel} from "../../model/UserModel";
import {ValidatorOrderWaitingFilter} from "../../model/ValidatorOrderWaitingFilter";
import {Campaign, Utils} from "../../model";
import {COMBO_LABELS} from "../../enum/comboLabels";

interface IViewModel extends ng.IController, IOrderFilterValidatorProps {
    applyFilter?();
    haveOneFilterActive(): boolean
    dropElement(item: any, key: string): void;
    lang: typeof lang;
    comboLabels: typeof COMBO_LABELS;
}

interface IOrderFilterValidatorProps {
    onSearch?;
    filter: ValidatorOrderWaitingFilter
    userList: Array<UserModel>;
    typeCampaignList: Array<Campaign>;
}

interface IOrderFilterValidatorScope extends IScope, IOrderFilterValidatorProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    filter: ValidatorOrderWaitingFilter;
    userList: Array<UserModel>;
    typeCampaignList: Array<Campaign>;

    lang: typeof lang;
    comboLabels: typeof COMBO_LABELS;

    constructor(private $scope: IOrderFilterValidatorScope,
                private $location: ILocationService,
                private $window: IWindowService) {
    }

    $onInit() {
        this.lang = lang;
        this.comboLabels = COMBO_LABELS;
        this.filter = new ValidatorOrderWaitingFilter(["users", "typeCampaign"])
    }

    $onDestroy() {
    }

    haveOneFilterActive(): boolean {
        return this.filter.users.length > 0 || this.filter.typeCampaign.length > 0;
    }

    dropElement(item: any, key: string): void {
        this.filter[key] = _.without(this.filter[key], item);
        this.$scope.vm.applyFilter();
    };
}

function directive($parse: IParseService) {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/order-filter-validator/order-filter-validator.html`,
        scope: {
            onSearch: '&',
            filter: '=',
            userList: '=',
            typeCampaignList: '=',
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', '$parse', Controller],
        /* interaction DOM/element */
        link: function ($scope: IOrderFilterValidatorScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
            vm.applyFilter = () => {
                if (vm.filter.startDate && vm.filter.endDate &&
                    moment(vm.filter.startDate).isSameOrBefore(moment(vm.filter.endDate))) {
                    $parse($scope.vm.onSearch())({});
                }
                Utils.safeApply($scope);
            };
        }
    }
}

export const orderFilterValidator = ng.directive('orderFilterValidator', directive)