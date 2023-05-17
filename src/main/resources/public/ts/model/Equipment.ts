import {idiom as lang, toasts} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http, {AxiosResponse} from 'axios';
import {Filters} from "./Filter";
import {Utils} from "./Utils";
import {Catalog, ICatalogResponse} from "./Catalog";
import {FiltersCatalogItem} from "./FiltersCatalogItem";
import {equipmentsService} from "../services/equipments.service";

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
    valid: boolean;

    constructor() {
        this._loading = false;
        this.valid = true;
    }

    async sync(id, idStructure?: string) {
        try {
            const stuctureParams = (idStructure) ? `?idStructure=${idStructure}` : ``;
            let url = `/crre/equipment/${id}${stuctureParams}`;
            let {data} = await http.get(url);
            Mix.extend(this, data);
            reformatEquipment(this);
            if (this.type === 'articlenumerique') {
                if (this.offres.length != 0) {
                    this.offres[0].leps.forEach(function (offre) {
                        offre.conditions.sort(function (a, b) {
                            return a.gratuite - b.gratuite;
                        });
                    });
                }
            }
        } catch (e) {
            console.error(e);
            toasts.warning('crre.equipment.sync.err');
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

function reformatEquipment(equipment: Equipment) {
    equipment.id = equipment.ean;
    equipment.status = equipment.disponibilite[0].valeur;
    equipment.commandable = equipment.disponibilite[0].commandable;
    equipment.description = equipment.description.replace(/\n/g, "<br>");
    let commentaire = equipment.disponibilite[0].commentaire
        .replace("À paraître", "").replace("Épuisé", "").replace("Epuisé", "Épuisé").replace("En cours de réimpression", "")
        .replace("Non disponible provisoirement", "").replace("En cours d'impression", "");
    if (commentaire) {
        if (commentaire == lang.translate(equipment.status)) {
            equipment.commentaire = null;
        } else {
            equipment.commentaire = commentaire;
        }
    } else {
        equipment.commentaire = equipment.status;
    }
    equipment.urlcouverture = equipment.urlcouverture.replace("cns-edu.org", "www.cns-edu.com");
}

export class Equipments extends Selection<Equipment> {
    page: number;
    _loading: boolean;
    all: Equipment[];
    page_count: number;
    filterFulfilled: boolean;
    filters: FiltersCatalogItem;

    constructor() {
        super([]);
        this.filters = new FiltersCatalogItem();
        this._loading = false;
        this.filterFulfilled = false;
    }

    syncEquip(catalog: Catalog, isFilter: boolean = false) {
        if (catalog.filters) {
            if (!this.filterFulfilled) {
                this.filters = catalog.filters;
                this.filterFulfilled = true;
            }
        }
        if (catalog.resources && catalog.resources.length > 0) {
            this.all = catalog.resources;
            if (!isFilter) {
                this.all.map((equipment: Equipment) => {
                    reformatEquipment(equipment);
                });
            }
        } else {
            this.all = [];
        }
    }

    async getEquipments(orders): Promise<any> {
        let params = '';
        let idsEquipments = [];
        orders.map((order) => {
            if (idsEquipments.indexOf(order.equipment_key) === -1) {
                idsEquipments.push(order.equipment_key);
            }
        });

        idsEquipments.map((id) => {
            params += `id=${id}&`;
        });


        params = params.slice(0, -1);
        let {data} = await http.get(`/crre/equipments?${params}`);
        this.all = Mix.castArrayAs(Equipment, data);
    }

    async getFilterEquipments(queryword?: string, filters?: Filters) {
        try {
            let uri: string;
            let params = "";
            if (filters) {
                params = "&";
                filters.all.forEach(function (f) {
                    params += f.name + "=" + f.value + "&";
                });
            }
            if (!Utils.format.test(queryword)) {
                if (!!queryword) {
                    uri = (`/crre/equipments/catalog/search?word=${queryword}${params}`);
                } else {
                    uri = (`/crre/equipments/catalog/filter?emptyFilter=${!this.filterFulfilled}${params}`);
                }
                await http.get(uri).then(async (res: AxiosResponse) => {
                    this.syncEquip(new Catalog().build(res.data));
                    this.loading = false;
                })
            } else {
                toasts.warning('crre.equipment.special');
            }
        } catch (e) {
            this.loading = false;
            toasts.warning('crre.equipment.sync.err');
            throw e;
        }
    }

    // Return all filters contained in catalog
    async getFilters() {
        try {
            let catalog: Catalog = await equipmentsService.getFilters();
            await this.syncEquip(catalog, true);
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
    value: number;
    id?: string;
    amount?: string;
    statut?: string;
    titre: string;
}

export class Offers {
    all: Offer[];

    constructor() {
        this.all = [];
    }
}