import {moment} from "entcore";
import {Order} from "./Order";

export class Utils {

    static parsePostgreSQLJson (json: any): any {
        try {
            if (json === '[null]') return [];
            let res = JSON.parse(json);
            if (typeof res !== 'string') {
                return res;
            }
            return JSON.parse(res);
        } catch (e) {
            return '';
        }
    }

    static safeApply ($scope: any) {
        let phase = $scope.$root.$$phase;
        if (phase !== '$apply' && phase !== '$digest') {
            $scope.$apply();
        }
    }

    static formatKeyToParameter (values: any[], key: string): string {
        let params: string = '';
        values.map((value) => params += value.hasOwnProperty(key) ? `${key}=${value[key]}&` : '');
        return params.slice(0, -1);
    }

    static calculatePriceTTC (price, tax_value, roundNumber?: number) {
        let priceFloat = parseFloat(price);
        let taxFloat = parseFloat(tax_value);
        let price_TTC = (( priceFloat + ((priceFloat *  taxFloat) / 100)));
        return (!isNaN(price_TTC)) ? (roundNumber ? price_TTC.toFixed(roundNumber) : price_TTC ) : '';
    }

    static formatGetParameters (obj: any): string {
        let parameters = '';
        Object.keys(obj).map((key) => {
            if (obj[key] == null || obj[key] === undefined) return;
            let type = obj[key].constructor.name;
            switch (type) {
                case 'Array' : {
                    obj[key].map((value) => parameters += `${key}=${value.toString()}&`);
                    break;
                }
                case 'Object': {
                    for (let innerKey in obj[key]) {
                        parameters += `${innerKey}=${obj[key][innerKey].toString()}&`;
                    }
                    break;
                }
                default: {
                    parameters += `${key}=${obj[key].toString()}&`;
                    break;
                }
            }
        });

        return parameters.slice(0, -1);
    }
    static formatDate (date:Date) {
        if (date === null) return '-';
        return moment(date).format('DD/MM/YY');
    }
    static formatDatePost (date:Date) {
        return  moment(date).format('YYYY-MM-DD');
    }
}