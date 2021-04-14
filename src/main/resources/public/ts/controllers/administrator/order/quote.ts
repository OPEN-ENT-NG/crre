import {ng, toasts} from "entcore";
import http from "axios";
import {INFINITE_SCROLL_EVENTER} from "../../../enum/infinite-scroll-eventer";
import {Utils} from "../../../model";

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
                toasts.warning('crre.quote.list.error');
            }
        };

        $scope.generateCSV = async(attachment:string, title:string) => {
            try {
                window.location = `/crre/quote/csv?attachment=${attachment}&title=${title}`;
            } catch (e) {
                toasts.warning('crre.quote.generate.csv.error');
            }
        };

        $scope.onScroll = async (): Promise<void> => {
            $scope.filter.page++;
            await $scope.search($scope.query_name);
        };

        function setQuotes(data) {
            data.map(quote => {
                let date = new Date(quote.creation_date);
                quote.creation_date = date.toLocaleDateString().replace(/\//g, "-") + " - " + date.toLocaleTimeString();
            });
            if (data.length > 0) {
                $scope.quotes = $scope.quotes.concat(data);
                $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            }
            $scope.loading = false;
            Utils.safeApply($scope);
        }

        const syncQuotes = async() => {
            let data = await $scope.getQuotes();
            setQuotes(data);
        };

        $scope.search = async (name: string, init: boolean = false) => {
            if(init) {
                $scope.quotes = [];
                $scope.filter.page = 0;
            }
            if (!!name) {
                let {data} = await http.get(`/crre/quote/search?q=${name}&page=${$scope.filter.page}`);
                setQuotes(data);
            } else {
                await syncQuotes();
                Utils.safeApply($scope);
            }
        };

        syncQuotes();

    }]);