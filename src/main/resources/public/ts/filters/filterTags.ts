import { ng, _ } from 'entcore';
export const tagFilter = ng.filter('tagFilter', () => {
    return (inputs, table) => {
       let newTags = _.difference(inputs, table);
        return newTags ;
    };
});