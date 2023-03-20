import {Equipment, Offer, Offers} from "./Equipment";
import {moment, idiom as lang} from "entcore";
import {Basket, BasketOrder, Baskets} from "./Basket";
import {OrderClient, OrdersClient} from "./OrderClient";
import {OrderRegion, OrdersRegion} from "./OrderRegion";
import {Project} from "./Project";

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

    static safeApply ($scope: any) : void {
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
            let array: Array<any> = []
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

    static isAvailable(equipment: Equipment) : boolean {
        try {
            let status_article : Array<string> = ["Disponible", "Précommande", "À paraître", "En cours de réimpression", "En cours d'impression",
                "Disponible jusqu'à épuisement des stocks", "À reparaître"];
            return equipment.disponibilite && equipment.disponibilite.length > 0 &&
                equipment.disponibilite[0].valeur && status_article.some((s: string) => s === equipment.disponibilite[0].valeur);
        } catch (e) {
            console.error("Error CRRE@isAvailable : " + e);
            return false;
        }
    }

    static calculatePriceTTC (equipment: Equipment, roundNumber?: number) : number {
        try {
            let prixHT: number = 0, price_TTC: number;
            let TVAs: any;

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

    static calculatePriceOfEquipments (baskets: Baskets, roundNumber?: number) : string {
        let totalPrice : number = 0;
        baskets.all.forEach((basket : Basket) => {
            if (Utils.isAvailable(basket.equipment) && (!this.hasOneSelected(baskets) || basket.selected)) {
                let basketItemPrice : string = this.calculatePriceOfBasket(basket,2);
                totalPrice += !isNaN(parseFloat(basketItemPrice)) ? parseFloat(basketItemPrice) : 0;
            }
        });
        return (!isNaN(totalPrice)) ? (roundNumber ? totalPrice.toFixed(roundNumber) : totalPrice.toString() ) : '0';
    }

    static calculatePriceOfBasket (basket: Basket, roundNumber?: number, toDisplay?: boolean) : string {
        let equipmentPrice : number = parseFloat(Utils.calculatePriceOfEquipment(basket.equipment, roundNumber));
        equipmentPrice = basket.amount === 0 && toDisplay ? equipmentPrice : equipmentPrice * basket.amount;
        return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice.toString()) : '0';
    };

    /**
     * Calculate the price of an equipment
     * @param {Equipment} equipment
     * @param {number} roundNumber [number of digits after the decimal point]
     * @returns {number}
     */
    static calculatePriceOfEquipment (equipment: Equipment, roundNumber?: number) : string {
        let price : number = Utils.calculatePriceTTC(equipment, roundNumber);
        return (!isNaN(price) && roundNumber) ? price.toFixed(roundNumber) : price.toString();
    };

    static hasOneSelected (baskets: Baskets) : boolean {
        let hasSelected : boolean = false;
        baskets.all.map((basket : Basket) => {
            if (basket.selected) { hasSelected = true; }
        });
        return hasSelected;
    };

    static setStatus(project : Project | BasketOrder, orders : OrdersRegion | Array<OrderClient>) : void {
        try {
            if (project && orders.length > 0 && orders[0].status) {
                project.status = orders[0].status;
                let partiallyRefused : boolean = false;
                let partiallyValidated : boolean = false;
                let partiallyResubmit : boolean = false;
                let partiallySent : boolean = false;
                if (orders.length > 1) {
                    for (const order of orders as Array<OrderRegion | OrderClient>) {
                        if (project.status != order.status) {
                            if (order.status == 'VALID' || project.status == 'VALID')
                                partiallyValidated = true;
                            else if (order.status == 'REJECTED' || project.status == 'REJECTED')
                                partiallyRefused = true;
                            else if (order.status == 'RESUBMIT' || project.status == 'RESUBMIT')
                                partiallyResubmit = true;
                            else if (order.status == 'IN_PROGRESS' || project.status == 'IN_PROGRESS')
                                partiallySent = true;
                        }
                    }
                    if (partiallyRefused || partiallyValidated || partiallyResubmit || partiallySent) {
                        for (const order of orders as Array<OrderRegion | OrderClient>) {
                            order.displayStatus = true;
                        }
                        if (partiallyRefused && !partiallyValidated)
                            project.status = "PARTIALLYREJECTED"
                        else if (partiallyValidated)
                            project.status = "PARTIALLYVALIDED"
                        else if (partiallyResubmit)
                            project.status = "PARTIALLYRESUBMIT"
                        else if (partiallySent)
                            project.status = "PARTIALLYSENT"
                    }
                }
            }
        } catch (e) {
            console.error("Error CRRE@setStatus : " + e);
        }
    }

    static computeOffer = (order : OrderRegion | OrderClient | Basket, equipment : Equipment,
                           offerStudent? : Array<string>, offerTeacher? : Array<string>) : Offers => {
        try {
            let gratuit : number = 0;
            let gratuite : number = 0;
            let offers : Offers = new Offers();
            if(!offerStudent || !offerTeacher){
                offerStudent = offerTeacher = [];
            }
            if (order.amount != undefined && equipment.offres && equipment.offres[0] && equipment.offres[0].leps &&
                equipment.offres[0].leps.length > 0) {
                let amount : number = order.amount;
                equipment.offres[0].leps.forEach(function (offer) {
                    let offre = new Offer();
                    if (offer.licence && offer.licence[0] && offer.licence[0].valeur != undefined) {
                        offre.name = offer.licence[0].valeur;
                    } else {
                        offre.name = "Offre gratuite";
                    }
                    let stringLicence : string = offre.name === "Offre gratuite" ?
                        lang.translate('crre.free.licences.for') : lang.translate('crre.free.licences.teacher.for');
                    if (offer.conditions) {
                        if (offer.conditions.length > 1) {
                            let conditionString : string = "";
                            offer.conditions.forEach(function (condition) {
                                if(offre.name === "Elève"){
                                    conditionString += condition.gratuite + lang.translate('crre.free.licences.student.for') +
                                        condition.conditionGratuite + lang.translate('crre.licences.buy') + ", ";
                                } else {
                                    conditionString += condition.gratuite + stringLicence +
                                        condition.conditionGratuite + lang.translate('crre.licences.buy') + ", ";
                                }
                                if (condition.conditionGratuite != undefined && condition.gratuite != undefined &&
                                    amount >= condition.conditionGratuite && gratuit < condition.conditionGratuite) {
                                    gratuit = condition.conditionGratuite;
                                    gratuite = condition.gratuite;
                                }
                            });
                            conditionString.slice(0, -2);
                            offre.name === "Elève" ? offerStudent.push(conditionString) : offerStudent.push(conditionString);
                        } else if (offer.conditions.length == 1 &&
                            offer.conditions[0].conditionGratuite && offer.conditions[0].gratuite != undefined) {
                            gratuit = offer.conditions[0].conditionGratuite;
                            const oneGratuite : number = offer.conditions[0].gratuite
                            gratuite = oneGratuite * Math.floor(amount / gratuit);
                            if(offre.name === "Elève") {
                                offerStudent.push(oneGratuite + lang.translate('crre.free.licences.student.for') +
                                    gratuit + lang.translate('crre.licences.buy'));
                            } else {
                                offerTeacher.push(oneGratuite + stringLicence + gratuit + lang.translate('crre.licences.buy'));
                            }
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

    static  getCurrentDate() : string {
        const MyDate : Date = new Date();
        let MyDateString : string;

        MyDateString = ('0' + MyDate.getDate()).slice(-2) + '/'
            + ('0' + (MyDate.getMonth()+1)).slice(-2) + '/'
            + MyDate.getFullYear();

        return MyDateString;
    }

    static formatDate(start: string, end: string) : {startDate : string, endDate : string} {
        let startDate : string = "", endDate : string = "";
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