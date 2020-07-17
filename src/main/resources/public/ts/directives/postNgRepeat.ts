import {ng,  idiom as lang} from 'entcore';
import {Utils} from "../model";

export const postNgRepeat = ng.directive('postNgRepeat', () => {
    return function(scope, element, attrs) {
        if (scope.$last){
            let elements = document.getElementsByClassName('vertical-array-scroll');
            elements[0].scrollLeft = 9999999999999 ;
        }
    }
});