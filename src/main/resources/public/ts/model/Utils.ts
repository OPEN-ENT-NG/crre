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
}