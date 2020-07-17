import { ng, idiom } from 'entcore';

export const currency = ng.filter('currency', () =>
    (amount) => {
        return (isNaN((parseFloat(amount))) ?
            0 :
            parseFloat(amount).toLocaleString(undefined,
                {minimumFractionDigits: 2, maximumFractionDigits: 2})) +
            ' ' + idiom.translate('money.symbol');
    }
);
