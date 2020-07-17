import { ng } from 'entcore';
import { Utils } from '../../model';

export const loggerController = ng.controller('loggerController', [
    '$scope', async ($scope) => {
        $scope.loadingArrayLogs = false;
        $scope.current = {
            page: 1
        };
        $scope.JSON = JSON;
        $scope.display = {
            lightbox: false
        };

        $scope.formatJson = (json: string, indent: number = 0) => {
            let value = JSON.stringify(Utils.parsePostgreSQLJson(json), undefined, indent);
            return value !== 'null' ? value : '';
        };

        $scope.parseJson = (json: string) => Utils.parsePostgreSQLJson(json);

        $scope.loadMoreLogs = async (page: number) => {
            $scope.loadingArrayLogs = true;
            Utils.safeApply($scope);
            $scope.current.page = page;
            await $scope.logs.loadPage(page);
            $scope.loadingArrayLogs = false;
            Utils.safeApply($scope);
        };

        $scope.nextPage = () => {
            $scope.loadMoreLogs(++$scope.current.page);
        };

        $scope.previousPage = () => {
            $scope.loadMoreLogs(--$scope.current.page);
        };

        $scope.showFile = (log: Log) => {
            $scope.log = log;
            $scope.display.lightbox = true;
        };

        $scope.getNumber = (size: number) => {
            let arr = [];
            for (let i = 1; i <= size; i++) {
                arr.push(i);
            }

            return arr;
        };

        // await $scope.logs.loadPage($scope.current.page);
        Utils.safeApply($scope);
    }
]);