/**
 * Created by rahnir on 22/01/2018.
 * return inputs that contains all filters
 * @table a table of filters "string"
 */
import { ng, _ } from 'entcore';
export const catalogSearchFilter = ng.filter('catalogSearchFilter', () => {

    return (inputs, table) => {
        let conditionFilter = (equipment, table) => {
            let result = [];
            let i = 0;
            do {
                result[i] = (table[i] !== '') ?
                    ((equipment.name ? equipment.name.toLowerCase().includes(table[i].toLowerCase())  : false )
                    || ( equipment.summary ? equipment.summary.toLowerCase().includes(table[i].toLowerCase()) : false )
                    || equipment.technical_specs.toString().includes(table[i].toLowerCase()) )
                    : true ;
                i++;
            }while (i < table.length && result[i > 1 ? i - 1 : 0] === true);

            return _.reduce(result, (oldVal, newVal ) => {return oldVal && newVal; }, true);
        };

       return table.length > 0
            ? _.without(inputs.map((input) => {
                if (input === undefined || conditionFilter(input, table)) return input ;
            }), undefined )
        :  inputs ;

    };
});