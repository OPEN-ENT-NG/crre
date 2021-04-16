import {Offer, Offers} from "./Equipment";
import {moment} from "entcore";

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
        if($scope && $scope.$root) {
            let phase = $scope.$root.$$phase;
            if (phase !== '$apply' && phase !== '$digest') {
                $scope.$apply();
            }
        }
    }

    static formatKeyToParameter (values: any[], key: string): string {
        let params: string = '';
        let array = []
        values.map((value) => {
            if(array.indexOf(value[key]) == -1) {
                params += value.hasOwnProperty(key) ? `${key}=${value[key]}&` : '';
                array.push(value[key]);
            }
        });
        return params.slice(0, -1);
    }

    static calculatePriceTTC (equipment, roundNumber?: number) {
        let prixht,price_TTC: number;
        let tvas;
        if(!equipment){
            return 0;
        }else {
            if (equipment.type == 'articlenumerique') {
                prixht = equipment.offres[0].prixht;
                tvas = equipment.offres[0].tvas;
            } else {
                prixht = equipment.prixht;
                tvas = equipment.tvas;
            }
            price_TTC = prixht;
            tvas.forEach((tva) => {
                let taxFloat = tva.taux;
                price_TTC += (((prixht) * tva.pourcent / 100) * taxFloat) / 100;
            });
            return (!isNaN(price_TTC)) ? (roundNumber ? parseFloat(price_TTC.toFixed(roundNumber)) : price_TTC) : 0;
        }
    }

    static setStatus(project, firstOrder) {
        project.status = firstOrder.status;
        let partiallyRefused = false;
        let partiallyValided = false;
        if(project.orders.length > 1){
            for (const order of project.orders) {
                if (project.status != order.status)
                    if (order.status == 'VALID' || project.status == 'VALID')
                        partiallyValided = true;
                    else if (order.status == 'REJECTED' || project.status == 'REJECTED')
                        partiallyRefused = true;
            }
            if (partiallyRefused || partiallyValided) {
                for (const order of project.orders) {
                    order.displayStatus = true;
                }
                if (partiallyRefused && !partiallyValided)
                    project.status = "PARTIALLYREJECTED"
                else
                    project.status = "PARTIALLYVALIDED"
            }
        }
    }

    static computeOffer = (order, equipment): Offers => {
        let amount = order.amount;
        let gratuit = 0;
        let gratuite = 0;
        let offre = null;
        let offers = new Offers();
        equipment.offres[0].leps.forEach(function (offer) {
            offre = new Offer();
            offre.name = "Manuel " + offer.licence[0].valeur;
            if(offer.conditions.length > 1) {
                offer.conditions.forEach(function (condition) {
                    if(amount >= condition.conditionGratuite && gratuit < condition.conditionGratuite) {
                        gratuit = condition.conditionGratuite;
                        gratuite = condition.gratuite;
                    }
                });
            } else {
                gratuit = offer.conditions[0].conditionGratuite;
                gratuite = offer.conditions[0].gratuite * Math.floor(amount/gratuit);
            }
            offre.value = gratuite;
            if(gratuite > 0) {
                offers.all.push(offre);
            }
        });
        return offers;
    };

    static formatDate(start: string, end: string) {
        const startDate = moment(start).format('YYYY-MM-DD').toString();
        const endDate = moment(end).format('YYYY-MM-DD').toString();
        return {startDate, endDate};
    };

    static format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
}