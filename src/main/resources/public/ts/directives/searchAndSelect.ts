import {$, idiom as lang, ng} from "entcore";

export const searchAndSelect = ng.directive('searchAndSelect', function() {
    return {
        restrict: 'E',
        replace: false,
        scope: {
            options: '=',
            ngModel: '=',
            param1: '@',
            param2: '@',
            searchOn: '@',
            orderBy: '@',
            disabled: '=',
            ngChange: '&',
            placeholder:'@'
        },
        templateUrl: '/lystore/public/template/directives/search-and-select/main.html',
        controller: ['$scope', '$filter', '$timeout', function($scope, $filter, $timeout) {
            /* Search input */
            $scope.search = {
                input: '',
                reset: function(){ this.input = "" }
            };
            $scope.lang = lang;
            /* Combo box visibility */
            $scope.show = false;
            if($scope.placeholder) {
                $scope.i18nplaceholder = lang.translate($scope.placeholder);
            }else{
                $scope.i18nplaceholder = lang.translate('Select');
            }
            $scope.toggleVisibility = function() {
                if(!$scope.disabled){
                    $scope.show = !$scope.show;
                    if ($scope.show) {
                        $scope.addClickEvent();
                        $scope.search.reset();
                        $timeout(function() {
                            $scope.setComboPosition()
                        }, 1)
                    }
                }
            };
            $scope.toggleItem = function(item) {
                $scope.ngModel = item;
                $scope.show = false;
            };
            $scope.fsearch = (item) => {
                if ($scope.search.input){
                    if($scope.param2)
                        return  (item[$scope.param2] ? (item[$scope.param2].toLowerCase()).includes($scope.search.input.toLowerCase()) : false)
                            || (item[$scope.param1] ? (item[$scope.param1].toLowerCase()).includes($scope.search.input.toLowerCase()) : false)
                    else
                        return  (item[$scope.param1] ? (item[$scope.param1].toLowerCase()).includes($scope.search.input.toLowerCase()) : false)
                }else
                    return true
            };

            /* Item display */
            $scope.display = function(item) {

                if($scope.param2)
                    return item instanceof Object ? item[$scope.param1] + " - " + item[$scope.param2] : item
                else
                    return item instanceof Object ? item[$scope.param1] : item
            };
            $scope.$watch(()=> $scope.ngModel, (newVal, oldVal)=>{
                if(newVal!=oldVal){
                    $scope.ngChange();
                }
            })
        }],
        link: function(scope, element, attributes) {
            if (!attributes.options ) {
                throw '[<search-and-select> directive] Error: combo-model & filtered-model attributes are required.'
            }

            /* Visibility mouse click event */
            scope.addClickEvent = function() {
                if (!scope.show)
                    return;

                let timeId = new Date().getTime();
                $('body').on('click.multi-combo' + timeId, function(e) {
                    if (!(element.find(e.originalEvent.target).length)) {
                        scope.show = false;
                        $('body').off('click.multi-combo' + timeId);
                        scope.$apply()
                    }
                })
            };

            /* Drop down position */
            scope.setComboPosition = function() {
                element.css('position', 'relative');
                element.find('.search-and-select-panel').css('top',
                    element.find('.search-and-select-button').outerHeight()
                )
            };
            scope.setComboPosition()
        }
    }
});