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
            console.error("Error CRRE@parsePostgreSQLJson : " + e);
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
        try {
            let params: string = '';
            let array = []
            values.map((value) => {
                if(array.indexOf(value[key]) == -1) {
                    params += value.hasOwnProperty(key) ? `${key}=${value[key]}&` : '';
                    array.push(value[key]);
                }
            });
            return params.slice(0, -1);
        } catch (e) {
            console.error("Error CRRE@formatKeyToParameter : " + e);
            return '';
        }
    }

    static isAvailable(equipment) : boolean {
        try {
            let status_article = ["Disponible", "Précommande", "À paraître", "En cours de réimpression", "En cours d'impression",
                "Disponible jusqu'à épuisement des stocks", "À reparaître"]
            return equipment.disponibilite && equipment.disponibilite.length > 0 &&
                equipment.disponibilite[0].valeur && status_article.some(s => s === equipment.disponibilite[0].valeur);
        } catch (e) {
            console.error("Error CRRE@isAvailable : " + e);
            return false;
        }
    }

    static calculatePriceTTC (equipment, roundNumber?: number) {
        try {
            let prixHT = 0, price_TTC: number;
            let TVAs;

            if (!equipment || !this.isAvailable(equipment)) {
                return 0;
            } else {
                if (equipment.type == 'articlenumerique' && equipment.offres && equipment.offres[0] &&
                    equipment.offres[0].prixht && equipment.offres[0].tvas) {
                    prixHT = equipment.offres[0].prixht;
                    TVAs = equipment.offres[0].tvas;
                } else if (equipment.prixht && equipment.tvas) {
                    prixHT = equipment.prixht;
                    TVAs = equipment.tvas;
                }
                price_TTC = prixHT;
                if (TVAs.length > 0) {
                    TVAs.forEach((tva) => {
                        if (tva.taux && tva.pourcent) {
                            let taxFloat = tva.taux;
                            price_TTC += (((prixHT) * tva.pourcent / 100) * taxFloat) / 100;
                        }
                    });
                }
                return (!isNaN(price_TTC)) ? (roundNumber ? parseFloat(price_TTC.toFixed(roundNumber)) : price_TTC) : 0;
            }
        } catch (e) {
            console.error("Error CRRE@calculatePriceTTC : " + e);
            return 0;
        }
    }

    static setStatus(project, firstOrder) {
        try {
            if (project && firstOrder && firstOrder.status) {
                project.status = firstOrder.status;
                let partiallyRefused = false;
                let partiallyValidated = false;
                if (project.orders && project.orders.length > 1) {
                    for (const order of project.orders) {
                        if (project.status != order.status)
                            if (order.status == 'VALID' || project.status == 'VALID')
                                partiallyValidated = true;
                            else if (order.status == 'REJECTED' || project.status == 'REJECTED')
                                partiallyRefused = true;
                    }
                    if (partiallyRefused || partiallyValidated) {
                        for (const order of project.orders) {
                            order.displayStatus = true;
                        }
                        if (partiallyRefused && !partiallyValidated)
                            project.status = "PARTIALLYREJECTED"
                        else
                            project.status = "PARTIALLYVALIDED"
                    }
                }
            }
        } catch (e) {
            console.error("Error CRRE@setStatus : " + e);
        }
    }

    static computeOffer = (order, equipment): Offers => {
        try {
            let gratuit = 0;
            let gratuite = 0;
            let offers = new Offers();
            if (order.amount != undefined && equipment.offres && equipment.offres[0] && equipment.offres[0].leps &&
                equipment.offres[0].leps.length > 0) {
                let amount = order.amount;
                equipment.offres[0].leps.forEach(function (offer) {
                    let offre = new Offer();
                    if (offer.licence && offer.licence[0] && offer.licence[0].valeur != undefined) {
                        offre.name = "Manuel(s) " + offer.licence[0].valeur;
                    } else {
                        offre.name = "Offre gratuite";
                    }
                    if (offer.conditions) {
                        if (offer.conditions.length > 1) {
                            offer.conditions.forEach(function (condition) {
                                if (condition.conditionGratuite != undefined && condition.gratuite != undefined &&
                                    amount >= condition.conditionGratuite && gratuit < condition.conditionGratuite) {
                                    gratuit = condition.conditionGratuite;
                                    gratuite = condition.gratuite;
                                }
                            });
                        } else if (offer.conditions.length == 1 &&
                            offer.conditions[0].conditionGratuite && offer.conditions[0].gratuite != undefined) {
                            gratuit = offer.conditions[0].conditionGratuite;
                            gratuite = offer.conditions[0].gratuite * Math.floor(amount / gratuit);
                        }
                    }
                    offre.value = gratuite;
                    if (gratuite > 0) {
                        offers.all.push(offre);
                    }
                });
            }
            return offers;
        } catch (e) {
            console.error("Error CRRE@computeOffer : " + e);
            return new Offers();
        }
    };

    static formatDate(start: string, end: string) {
        let startDate = "", endDate = "";
        try {
            startDate = moment(start).format('YYYY-MM-DD').toString();
            endDate = moment(end).format('YYYY-MM-DD').toString();
            return {startDate, endDate};
        } catch (e) {
            console.error("Error CRRE@formatDate : " + e);
            return {startDate, endDate};
        }
    };

    static format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
}