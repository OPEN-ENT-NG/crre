import {ng, toasts} from "entcore";
import http from "axios";
import {INFINITE_SCROLL_EVENTER} from "../../enum/infinite-scroll-eventer";
import {Utils} from "../../model";
import {attachment} from "entcore/types/src/ts/editor/options";

declare let window: any;
export const quoteController = ng.controller('quoteController',
    ['$scope', ($scope) => {
        $scope.quotes = [];
        $scope.filter = {
            page: 0
        };
        $scope.loading = true;

        $scope.getQuotes = async() => {
            try {
                const page: string = $scope.filter.page ? `?page=${$scope.filter.page}` : '';
                let {data} = await http.get(`/crre/quote${page}`);
                return data;
            } catch (e) {
                toasts.warning('TODO');
            }
        }

        $scope.generateCSV = async(attachment:string) => {
            try {
                window.location = `/crre/quote/csv?attachment=${attachment}`;
            } catch (e) {
                toasts.warning('TODO');
            }
        }
        const syncQuotes = async() => {
            let data = await $scope.getQuotes();
            if(data.length > 0 ) {
                $scope.quotes = $scope.quotes.concat(data);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            Utils.safeApply($scope);
        }

        syncQuotes();

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.search($scope.query_name);
        };

        $scope.search = async (name: string, init: boolean = false) => {
            if(init) {
                $scope.quotes = [];
                $scope.filter.page = 0;
            }
            if (!!name) {
                let {data} = await http.get(`/crre/quote/search?q=${name}&page=${$scope.filter.page}`);
                if(data.length > 0 ) {
                    $scope.quotes = $scope.quotes.concat(data);
                    $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
                }
                Utils.safeApply($scope);
            } else {
                await syncQuotes();
                Utils.safeApply($scope);
                }
            }
    }])