import {ng,  idiom as lang} from 'entcore';

export const parameterTable = ng.directive('parameterTable', () => {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            ngChange: '&',
            ngDisabled: '@'
        },
        template: `
        <div class="parameter-table" ng-class="{'show':showArticle, 'hide':!showArticle && hideArticle }" ng-click="hideArticle = true">
            <div class="button-puce cell" ng-click="showArticle= !showArticle">
                <i class="param"></i>
            </div>
            <article class="cell" ng-if="showArticle">
                   <div ng-repeat=" field in ngModel" class="vertical-spacing-5">
                    <label >
                        <input type="checkbox" ng-model="field.display" ng-change="valueChange()" ng-disabled="ngDisabled"/>
                         <span><i18n>[[translate(field.name)]]</i18n></span>
                     </label>
                    </div>
            </article>
        </div>
        `,
        link: function ($scope, $element, $attrs) {
            $scope.valueChange = function() {
                setTimeout(function() {
                    if ($attrs.ngChange) $scope.$parent.$eval($attrs.ngChange);
                }, 0);
            };
        $scope.showArticle = false;
        $scope.hideArticle = false;
        $scope.translate = lang.translate
        }
    };
});