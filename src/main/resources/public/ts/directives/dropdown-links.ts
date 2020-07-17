import { ng, $ } from 'entcore';

export const dropDownMenu = ng.directive('dropDownMenu', () => {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            title: '@'
        },
        template: '' +
        '<div class="title">[[title]]</div>' +
        '<div class="options hidden" ng-transclude></div>',
        link: (scope, element, attributes) => {
            element.children('.options').on('mouseover', (e) => {
                e.stopPropagation();
            });

            element.children('.title').on('click', () => {
                if (element.children('.options').hasClass('hidden')) {
                    element.children('.options').removeClass('hidden');
                } else {
                    element.children('.options').addClass('hidden');
                }
            });

            $('body').click((e) => {
                if (e.target === element.find('.title')[0] ||
                    element.children('.options').hasClass('hidden')) {
                    return;
                }

                element.children('.options').addClass('hidden');
            });
        }
    };
});