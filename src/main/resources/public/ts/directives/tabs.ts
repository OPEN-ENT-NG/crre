import {ng, idiom as lang} from 'entcore';

export const Tabs = ng.directive('tabs', () => {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            ngChange:"&",
            menus: '='
        },
        template: `
        <div class="tabs-container row">
            <div ng-repeat="menu  in menus" class="menu aligned" ng-class="{'selected': ngModel == menu.value}" ng-click="selectMenu(menu)">
                [[lang.translate(menu.name)]]
            </div>
        </div>
        `,
        link: function ($scope, $element, $attrs, ngModel) {
            $scope.lang = lang;
            $scope.selectMenu =(menu)=>{
                let oldModel = $scope.ngModel;
                $scope.ngModel = menu.value;
                if(oldModel != $scope.ngModel && $scope.ngChange){
                    $scope.$apply();
                    $scope.ngChange();

                }
            }
        }
    };
});