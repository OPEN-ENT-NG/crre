import {moment, ng, idiom as lang, template, _} from "entcore";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {Campaign, Equipment, Structure, Utils} from "../../model";
import {ProjectFilter} from "../../model/ProjectFilter";
import {COMBO_LABELS} from "../../enum/comboLabels";
import {ORDER_STATUS_ENUM} from "../../enum/order-status-enum";
import {StatusFilter} from "../../model/StatusFilter";

interface IViewModel extends ng.IController, IProjectFilterAdminProps {
    applyFilter?();
    openFiltersLightbox(): void
    closeLightbox(): void;
    lang: typeof lang;
    comboLabels: typeof COMBO_LABELS;
    displayLightboxWaitingAdmin: boolean;
}

interface IProjectFilterAdminProps {
    onSearch?;
    filter: ProjectFilter
    campaignList: Array<Campaign>;
    statusFilterList: Array<StatusFilter>;
    structureList: Array<Structure>;
    catalogList: Array<{name}>;
    schoolType: Array<{name}>;
    editorList: Array<String>;
    distributorList: Array<String>;
}

interface IProjectFilterAdminScope extends IScope, IProjectFilterAdminProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    filter: ProjectFilter
    campaignList: Array<Campaign>;
    statusFilterList: Array<StatusFilter>;
    structureList: Array<Structure>;
    catalogList: Array<{name}>;
    schoolType: Array<{name}>;
    editorList: Array<String>;
    distributorList: Array<String>;

    lang: typeof lang;
    comboLabels: typeof COMBO_LABELS;
    displayLightboxWaitingAdmin: boolean;

    constructor(private $scope: IProjectFilterAdminScope,
                private $location: ILocationService,
                private $window: IWindowService) {
        this.lang = lang;
        this.comboLabels = COMBO_LABELS;
    }

    $onInit() {
        this.filter.statusFilterList = this.statusFilterList.filter((statusFilter: StatusFilter) => ORDER_STATUS_ENUM.VALID === statusFilter.orderStatusEnum || ORDER_STATUS_ENUM.IN_PROGRESS === statusFilter.orderStatusEnum)
        this.filter.filterChoiceCorrelation = new Map<string, string>([
            ["campaignList", "id_campaign"], ["statusFilterList", "status"], ["structureList", "id_structure"],
                ["catalogList", "_index"], ["schoolType", "type"], ["editorList", "editeur"], ["distributorList", "distributeur"]
        ]);
        Utils.safeApply(this.$scope);
    }

    $onDestroy() {
    }

    haveOneFilterActive(): boolean {
        return Array.from(this.filter.filterChoiceCorrelation.keys()).find((value: string) => this.filter[value].length > 0) != null;
    }

    dropElement(item: any, key: string): void {
        this.filter[key] = _.without(this.filter[key], item);
        this.$scope.vm.applyFilter();
    }

    openFiltersLightbox(): void {
        template.open('lightbox.moreFilter', 'administrator/order/filters')
            .then(() => {
                this.displayLightboxWaitingAdmin = true;
                Utils.safeApply(this.$scope);
            })
            .catch(err => console.error("Error when opening template: " + err));
    }

    closeLightbox(): void {
        this.displayLightboxWaitingAdmin = false;
        this.$scope.vm.applyFilter();
    }
}

function directive($parse: IParseService) {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/project-filter-admin/project-filter-admin.html`,
        scope: {
            onSearch: '&',
            filter: '=',
            campaignList: '=',
            statusFilterList: '=',
            structureList: '=',
            catalogList: '=',
            schoolType: '=',
            editorList: '=',
            distributorList: '=',
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', '$parse', Controller],
        /* interaction DOM/element */
        link: function ($scope: IProjectFilterAdminScope,
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

export const projectFilterAdmin = ng.directive('projectFilterAdmin', directive)