import {_, toasts} from 'entcore';
import {Eventer, Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';

export class Equipment implements Selectable {
    id?: string;
    ean: string;
    summary: string;
    description: string;
    price: number;
    status: string;
    urlcouverture: string;
    technical_specs: TechnicalSpec[];
    selected: boolean;
    eventer: Eventer;
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

    constructor () {
        this.eventer = new Eventer();
        this._loading = false;
        this.technical_specs = [];
    }

    async sync (id) {
        this.loading = true;

        try {
            let { data } =  await http.get(`/crre/equipment/${id}`);
             Mix.extend(this, data[0]);
                this.id = this.ean;
                this.status = this.disponibilite[0].valeur;
                if(this.disciplines.length != 0) {
                    this.discipline = this.disciplines[0].libelle;
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
        this.eventer.trigger(`loading::${this._loading}`);
    }

    get loading() {
        return this._loading;
    }

}

export class TechnicalSpec {
    name: string;
    value: string;
    constructor(){
    }
    toJson () {
        return {
            name: this.name,
            value: this.value
        };
    }
    toString () {
        return this.name + ' ' + this.value;
    }
}

export interface Equipments {
    eventer: Eventer;
    page: number;
    _loading: boolean;
    all: Equipment[];
    page_count: number;
    subjects: String[];
    grades: String[];
    editors: String[];
    os: String[];
    public: String[];

    sort: {
        type: string,
        reverse: boolean,
        filters: string[]
    }
}

export class Equipments extends Selection<Equipment> {
    constructor() {
        super([]);
        this.eventer = new Eventer();
        this.subjects = [];
        this.grades = [];
        this.os = [];
        this.public = [];
        this.editors = [];
        this._loading = false;
        this.sort = {
            type: 'name',
            reverse: false,
            filters: []
        };
    }

    async syncEquip (data: any) {
        if(data.length > 0 ) {
            if(data[0].hasOwnProperty("ressources")) {
                let filters = data[1].filters[0];
                this.subjects = filters.disciplines.map(v => ({name:v}));
                this.grades = filters.niveaux.map(v => ({name:v}));
                this.os = filters.os.map(v => ({name:v}));
                this.public = filters.public.map(v => ({name:v}));
                this.editors = filters.editors.map(v => ({name:v}));
                data = data[0].ressources;
            }
            this.all = Mix.castArrayAs(Equipment, data);
            this.all.map((equipment) => {
                equipment.id = equipment.ean;
                equipment.status = equipment.disponibilite[0].valeur;
                if(equipment.disciplines.length != 0) {
                    equipment.discipline = equipment.disciplines[0].libelle;
                }
            });
        } else {
            this.all = [];
        }

    }

    async getFilterEquipments(word: string, filter?: string){
        this.loading = true;
        try {
            let uri: string;
            var format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
            if(!format.test(word)) {
                if(filter) {
                    if(!!word) {
                        uri = (`/crre/equipments/catalog/filter?filter=${filter}&word=${word}`);
                    } else {
                        uri = (`/crre/equipments/catalog/filter?filter=${filter}`);
                    }
                } else {
                    if(!!word) {
                        uri = (`/crre/equipments/catalog/search?word=${word}`);
                    } else {
                        uri = (`/crre/equipments/catalog`);
                    }
                }
                let {data} = await http.get(uri);
                this.syncEquip(data);
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
        this.loading = true;
        try {
            let {data} = await http.get(`/crre/equipments/catalog`);
            this.syncEquip(data);

        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        } finally {
            this.loading = false;
        }
    }

    set loading(state: boolean) {
        this._loading = state;
        if(this.eventer)
            this.eventer.trigger(`loading::${this._loading}`);
    }

    get loading() {
        return this._loading;
    }
}