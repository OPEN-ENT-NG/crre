import {Behaviours, ng} from 'entcore';
import {RootsConst} from "../../core/constants/roots.const";
import {IScope} from "angular";

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    scroll(): Promise<void>;

    loading: boolean;

    loadingMode: boolean;
}

interface ICrreScope extends IScope {
    vm: IViewModel;
}

class Controller implements ng.IController, IViewModel {
    loading: boolean;
    loadingMode: boolean;

    private currentscrollHeight: number = 0;
    // latest height once scroll will reach
    private latestHeightBottom: number = 300;
    private scrolled: () => void;

    constructor(private $scope: ICrreScope) {
    }

    $onInit = () => {
        this.loading = false;
        $(window).on("scroll", async () => {
            await this.scroll();
        });
        // If somewhere in your controller you have to reinitialise anything that should "reset" your dom height
        // We reset currentscrollHeight
        Behaviours.applicationsBehaviours['crre'].InfiniteScrollService
            .getInfiniteScroll().subscribe(() => this.currentscrollHeight = 0);
        this.scroll();
    };

    scroll = async (): Promise<void> => {
        const scrollHeight: number = $(document).height() as number;
        const scrollPos: number = Math.floor($(window).height() + $(window).scrollTop());
        const isBottom: boolean = scrollHeight - this.latestHeightBottom < scrollPos;

        if (isBottom && this.currentscrollHeight < scrollHeight) {
            if (this.loadingMode) this.loading = true;
            await this.scrolled();
            if (this.loadingMode) this.loading = false;
            // Storing the latest scroll that has been the longest one in order to not redo the scrolled() each time
            this.currentscrollHeight = scrollHeight;
        }
    }

    $onDestroy = () => {
        $(window).off("scroll");
    };

}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/infinite-scroll/infinite-scroll.html`,
        scope: {
            scrolled: '&',
            loadingMode: '=?'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', Controller],
        /* interaction DOM/element */
        link: function (scope: ng.IScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: ng.IController) {
        }
    }
}

export const infiniteScroll = ng.directive('infiniteScrollV2', directive)