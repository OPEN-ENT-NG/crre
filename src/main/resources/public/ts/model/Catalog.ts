import {Equipment} from "./Equipment";
import {FiltersCatalogItem, IFiltersCatalogItem} from "./FiltersCatalogItem";

export interface ICatalogResponse {
    resources: Equipment[];
    filters: IFiltersCatalogItem;
}

export class Catalog {

    private _resources: Equipment[];
    private _filters: FiltersCatalogItem;

    constructor() {
        this._resources = [];
        this._filters = new FiltersCatalogItem();
    }

    build(data: ICatalogResponse): Catalog {
        this._resources = data.resources ? data.resources : [];
        this._filters = data.filters ? this.filters.build(data.filters) : new FiltersCatalogItem();
        return this;
    }

    get filters(): FiltersCatalogItem {
        return this._filters;
    }

    set filters(value: FiltersCatalogItem) {
        this._filters = value;
    }
    get resources(): Equipment[] {
        return this._resources;
    }

    set resources(value: Equipment[]) {
        this._resources = value;
    }
}