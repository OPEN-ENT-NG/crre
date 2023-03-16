import {_, moment, ng, idiom as lang} from "entcore";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {UserModel} from "../../model/UserModel";
import {ValidatorOrderWaitingFilter} from "../../model/ValidatorOrderWaitingFilter";
import {Campaign, Utils} from "../../model";
import {COMBO_LABELS} from "../../enum/comboLabels";

interface IViewModel extends ng.IController, IOrderWaitingFilterValidatorProps {
    applyFilter?();
    haveOneFilterActive(): boolean
    dropElement(item: any, key: string): void;
    lang: typeof lang;
    comboLabels: typeof COMBO_LABELS;
}

interface IOrderWaitingFilterValidatorProps {
    onSearch?;
    filter: ValidatorOrderWaitingFilter
    userList: Array<UserModel>;
    typeCampaignList: Array<Campaign>;
}

interface IOrderWaitingFilterValidatorScope extends IScope, IOrderWaitingFilterValidatorProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    filter: ValidatorOrderWaitingFilter;
    userList: Array<UserModel>;
    typeCampaignList: Array<Campaign>;

    lang: typeof lang;
    comboLabels: typeof COMBO_LABELS;

    constructor(private $scope: IOrderWaitingFilterValidatorScope,
                private $location: ILocationService,
                private $window: IWindowService) {
    }

    $onInit() {
        this.lang = lang;
        this.comboLabels = COMBO_LABELS;
        this.filter = new ValidatorOrderWaitingFilter();
        this.filter.filterChoiceCorrelation = new Map<string, string>([["userList", "id_user"], ["typeCampaignList", "id_campaign"]]);
    }

    $onDestroy() {
    }

    haveOneFilterActive(): boolean {
        return this.filter.userList.length > 0 || this.filter.typeCampaignList.length > 0;
    }

    dropElement(item: any, key: string): void {
        this.filter[key] = _.without(this.filter[key], item);
        this.$scope.vm.applyFilter();
    };
}

function directive($parse: IParseService) {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/order-waiting-filter-validator/order-waiting-filter-validator.html`,
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
        link: function ($scope: IOrderWaitingFilterValidatorScope,
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

export const orderWaitingFilterValidator = ng.directive('orderWaitingFilterValidator', directive)