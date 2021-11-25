import {idiom as lang, toasts} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';
import {Filters} from "./Filter";
import {Utils} from "./Utils";

export class Equipment implements Selectable {
    id?: string;
    ean: string;
    summary: string;
    description: string;
    price: number;
    status: string;
    urlcouverture: string;
    selected: boolean;
    _loading: boolean;
    priceTTC?: number;
    disponibilite: any[];
    disciplines: any[];
    discipline: string;
    titre: string;
    ark: string;
    type: string;
    offres: any;
    prixht: number;
    tvas: any;
    commentaire: string;
    commandable: boolean;

    constructor () {
        this._loading = false;
    }

    async sync (id, idStructure?:string) {
        try {
            const stuctureParams = (idStructure) ? `?idStructure=${idStructure}` : ``;
            let url = `/crre/equipment/${id}${stuctureParams}`;
            let { data } =  await http.get(url);
            Mix.extend(this, data);
            reformatEquipment(this);
            if(this.type === 'articlenumerique') {
                this.offres[0].leps.forEach(function (offre) {
                    offre.conditions.sort(function (a, b) {
                        return a.gratuite - b.gratuite;
                    });
                });
            }
        } catch (e) {
            console.error(e);
            toasts.warning('crre.equipment.sync.err');
        }
        finally {
            this.loading = false;
        }
    }

    set loading(state: boolean) {
        this._loading = state;
    }

    get loading() {
        return this._loading;
    }

}

function reformatEquipment(equipment: Equipment) {
    equipment.id = equipment.ean;
    equipment.status = equipment.disponibilite[0].valeur;
    equipment.commandable = equipment.disponibilite[0].commandable;
    let commentaire = equipment.disponibilite[0].commentaire
        .replace("À paraître", "").replace("Épuisé", "").replace("Epuisé", "Épuisé").replace("En cours de réimpression", "")
        .replace("Non disponible provisoirement", "").replace("En cours d'impression", "");
    if (commentaire) {
        if(commentaire == lang.translate(equipment.status)) {
            equipment.commentaire = null;
        }else{
            equipment.commentaire = commentaire;
        }
    } else {
        equipment.commentaire = equipment.status;
    }
    if (equipment.disciplines.length != 0) {
        equipment.discipline = equipment.disciplines[0].libelle;
    }
    equipment.urlcouverture = equipment.urlcouverture.replace("cns-edu.org","www.cns-edu.com");
}

export class Equipments extends Selection<Equipment> {
    page: number;
    _loading: boolean;
    all: Equipment[];
    page_count: number;
    subjects: String[];
    grades: String[];
    editors: String[];
    os: String[];
    public: String[];
    docsType: any;
    filterFulfilled: boolean;
    distributeurs: String[];

    constructor() {
        super([]);
        this.subjects = [];
        this.grades = [];
        this.os = [];
        this.public = [];
        this.editors = [];
        this.docsType = [];
        this.distributeurs = [];
        this._loading = false;
        this.filterFulfilled = false;
    }

    async syncEquip (data: any) {

        function setFilterValues(filters, group) {
            this[group] = filters[group].map(v => ({name: v}));
            if(group === 'public'){
                this[group].forEach((item) => item.toString = () => lang.translate(item.name));
            }else {
                this[group].forEach((item) => item.toString = () => item.name);
            }
        }

        if(data.length > 0 ) {
            if(data[0].hasOwnProperty("ressources")) {
                if(!this.filterFulfilled) {
                    let filters = data[1].filters[0];
                    setFilterValues.call(this, filters, 'subjects');
                    setFilterValues.call(this, filters, 'grades');
                    setFilterValues.call(this, filters, 'os');
                    setFilterValues.call(this, filters, 'public');
                    setFilterValues.call(this, filters, 'editors');
                    setFilterValues.call(this, filters, 'distributeurs');
                    this.docsType = [{name: "articlepapier"}, {name: "articlenumerique"}];
                    this.docsType.forEach((item) => item.toString = () => lang.translate(item.name));
                    this.filterFulfilled = true;
                }
                data = data[0].ressources;
            }
            this.all = Mix.castArrayAs(Equipment, data);
            this.all.map((equipment) => {
                reformatEquipment(equipment);
            });
        } else {
            this.all = [];
        }
    }

    async getEquipments(orders) :Promise <any> {
        let params = '';
        orders.map((order) => {
            params += `id=${order.equipment_key}&`;
        });
        params = params.slice(0, -1);
        let {data} = await http.get(`/crre/equipments?${params}`);
        this.all = Mix.castArrayAs(Equipment, data);
    }

    async getFilterEquipments(queryword?: string, filters?: Filters){
        try {
            let uri: string;
            let params = "";
            if(filters) {
                params = "&";
                filters.all.forEach(function (f) {
                    params += f.name + "=" + f.value + "&";
                });
            }
            if(!Utils.format.test(queryword)) {
                if(!!queryword) {
                    uri = (`/crre/equipments/catalog/search?word=${queryword}${params}`);
                } else {
                    uri = (`/crre/equipments/catalog/filter?emptyFilter=${!this.filterFulfilled}${params}`);
                }
                let {data} = await http.get(uri);
                await this.syncEquip(data);
            } else {
                toasts.warning('crre.equipment.special');
            }
        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        } finally {
            this.loading = false;
        }
    }

    async sync() {
        try {
            let {data} = await http.get(`/crre/equipments/catalog`);
            await this.syncEquip(data);

        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        } finally {
            this.loading = false;
        }
    }

    set loading(state: boolean) {
        this._loading = state;
    }

    get loading() {
        return this._loading;
    }
}

export class Offer {
    name: string;
    value: string;
    id?: string;
    amount?: string;
    statut?: string;
}

export class Offers {
    all: Offer[];

    constructor() {
        this.all = [];
    }
}