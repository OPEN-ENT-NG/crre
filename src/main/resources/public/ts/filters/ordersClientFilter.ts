/**
 * Created by rahnir on 22/01/2018.
 * return inputs that contains all filters
 * @table a table of filters "string"
 */
import { ng, _ } from 'entcore';
export const ordersClientFilter = ng.filter('ordersClientFilter', () => {
    return (inputs, status, table) => {
        let filter = (order, status, table) => {
            let result = [];
            let i = 0;
            do {
                result[i] = (table[i] !== '') ?
                    conditionFilter (order, status, table[i])
                    : true ;
                i++;
            }while (i < table.length && result[i > 1 ? i - 1 : 0] === true);

            return _.reduce(result, (oldVal, newVal ) => {return oldVal && newVal; }, true);
        };
        let conditionFilter = (order, status, table) => {
            switch (status) {
                case 'WAITING': {
                    return   ((order.name_structure ? order.name_structure.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || ( order.contract ? order.contract.name.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || (order.supplier ? order.supplier.name.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || (order.campaign ? order.campaign.name.toString().toLowerCase().includes(table.toLowerCase()) : false) );
                }
                case 'VALID': {
                    return ((order.name_structure ? order.name_structure.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || ( order.contract ? order.contract.name.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || (order.supplier ? order.supplier.name.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || (order.number_validation ? order.number_validation.toLowerCase().includes(table.toLowerCase()) : false ));
                }
                case 'SENT': {
                    return ((order.name_structure ? order.name_structure.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || ( order.contract ? order.contract.name.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || (order.supplier ? order.supplier.name.toString().toLowerCase().includes(table.toLowerCase()) : false )
                        || (order.number_validation ? order.number_validation.toLowerCase().includes(table.toLowerCase()) : false ));
                }
            }
        };
        return table && table.length > 0
            ? _.without(inputs.map((input) => {
                if (input === undefined || filter(input, status, table)) return input ;
            }), undefined )
            :  inputs ;

    };
});