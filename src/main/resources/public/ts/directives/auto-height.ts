import {ng} from 'entcore';

export const autoHeight = ng.directive('autoHeight', () => {
    return {
        restrict: 'A',
        link: function ($scope, $element) {
            const manageTextareaHeight = () => {
                $element.css({height: "1px"});
                $element.css({height: (25 + $element[0].scrollHeight) + "px"});
            };

            $element.bind('keyup', function () {
                manageTextareaHeight();
            });

            setTimeout(() => {
                manageTextareaHeight();
                $element.focus();
            }, 100);
        }
    };
});