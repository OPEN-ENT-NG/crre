import {ng} from 'entcore';

export const postNgRepeat = ng.directive('postNgRepeat', () => {
    return function(scope) {
        if (scope.$last){
            let elements = document.getElementsByClassName('vertical-array-scroll');
            if(elements && elements[0])
                elements[0].scrollLeft = 9999999999999 ;
        }
    }
});