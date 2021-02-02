import {_, ng} from 'entcore';

export const tagFilter = ng.filter('tagFilter', () => {
    return (inputs, table) => {
        return _.difference(inputs, table) ;
    };
});