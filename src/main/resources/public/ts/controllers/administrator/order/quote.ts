import {ng} from "entcore";
import {INFINITE_SCROLL_EVENTER} from "../../../enum/infinite-scroll-eventer";
import {Quotes, Utils} from "../../../model";

export const quoteController = ng.controller('quoteController',
    ['$scope', async ($scope) => {
        $scope.quotes = new Quotes();
        $scope.filter = {
            page: 0
        };
        $scope.loading = true;
        $scope.allSelected=false;

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.search($scope.query_name);
        };

        $scope.search = async (name: string, init: boolean = false) => {
            if (init) {
                $scope.quotes = [];
                $scope.filter.page = 0;
            }
            let newData: boolean;
            if (!!name) {
                newData = await $scope.quotes.get(name, $scope.filter.page);
            } else {
                newData = await $scope.quotes.get($scope.filter.page);
                Utils.safeApply($scope);
            }
            if (newData) {
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            Utils.safeApply($scope);
        };

        $scope.switchAll = () => {
            $scope.quotes.all.forEach(project => {
                project.selected = $scope.allSelected;
            });
            Utils.safeApply($scope);
        };

        let newData = await $scope.quotes.get($scope.filter.page);
        if (newData) {
            $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
        }
        $scope.loading = false;
        Utils.safeApply($scope);

    }]);