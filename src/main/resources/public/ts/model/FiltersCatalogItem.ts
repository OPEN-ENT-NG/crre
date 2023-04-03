import {idiom as lang} from "entcore";

export interface IFiltersCatalogItem {
    disciplines: Array<string>;
    grades: Array<string>;
    classes: Array<string>;
    devices: Array<string>;
    targets: Array<string>;
    editors: Array<string>;
    distributors: Array<string>;
    catalogs: Array<string>;
    pros: Array<string>;
    consumables: Array<string>;
}

export class FilterCatalogItem {
    public name: string;
    public toString?: () => string;
    public nameFormat?: string;

    constructor(name: string) {
        this.name = name;
    }
}

export class FiltersCatalogItem {
    private _disciplines: FilterCatalogItem[];
    private _grades: FilterCatalogItem[];
    private _classes: FilterCatalogItem[];
    private _devices: FilterCatalogItem[];
    private _targets: FilterCatalogItem[];
    private _editors: FilterCatalogItem[];
    private _distributors: FilterCatalogItem[];
    private _catalogs: FilterCatalogItem[];
    private _pros: FilterCatalogItem[];
    private _consumables: FilterCatalogItem[];

    constructor() {
        this._disciplines = [];
        this._grades = [];
        this._classes = [];
        this._devices = [];
        this._targets = [];
        this._editors = [];
        this._distributors = [];
        this._catalogs = [];
        this._pros = [];
        this._consumables = [];
    }

    public build(filters: IFiltersCatalogItem): FiltersCatalogItem {
        this._disciplines = (filters.disciplines || []).map((discipline: string) => {
            const item = new FilterCatalogItem(discipline);
            item.toString = () => item.name;
            item.nameFormat = item.name.replace("É", "E");
            return item;
        });
        this._grades = (filters.grades || []).map((grade: string) => {
            const item = new FilterCatalogItem(grade);
            item.toString = () => item.name;
            return item;
        });
        this._classes = (filters.classes || []).map((c: string) => {
            const item = new FilterCatalogItem(c);
            item.toString = () => item.name;
            return item;
        });
        this._devices = (filters.devices || []).map((device: string) => {
            const item = new FilterCatalogItem(device);
            item.toString = () => item.name;
            return item;
        });
        this._targets = (filters.targets || []).map((target: string) => {
            const item = new FilterCatalogItem(target);
            item.toString = () => item.name;
            return item;
        });
        this._editors = (filters.editors || []).map((editor: string) => {
            const item = new FilterCatalogItem(editor);
            item.toString = () => item.name;
            item.nameFormat = item.name.replace("L’é", "e");
            return item;
        });
        this._distributors = (filters.distributors || []).map((distributor: string) => {
            const item = new FilterCatalogItem(distributor);
            item.toString = () => item.name;
            return item;
        });

        this._catalogs = (filters.catalogs || []).map((catalog: string) => {
            const item = new FilterCatalogItem(catalog);
            item.toString = () => lang.translate(item.name);
            return item;
        });

        return this;
    }


    get disciplines(): FilterCatalogItem[] {
        return this._disciplines;
    }

    set disciplines(value: FilterCatalogItem[]) {
        this._disciplines = value;
    }

    get grades(): FilterCatalogItem[] {
        return this._grades;
    }

    set grades(value: FilterCatalogItem[]) {
        this._grades = value;
    }

    get classes(): FilterCatalogItem[] {
        return this._classes;
    }

    set classes(value: FilterCatalogItem[]) {
        this._classes = value;
    }

    get devices(): FilterCatalogItem[] {
        return this._devices;
    }

    set devices(value: FilterCatalogItem[]) {
        this._devices = value;
    }

    get targets(): FilterCatalogItem[] {
        return this._targets;
    }

    set targets(value: FilterCatalogItem[]) {
        this._targets = value;
    }

    get editors(): FilterCatalogItem[] {
        return this._editors;
    }

    set editors(value: FilterCatalogItem[]) {
        this._editors = value;
    }

    get distributors(): FilterCatalogItem[] {
        return this._distributors;
    }

    set distributors(value: FilterCatalogItem[]) {
        this._distributors = value;
    }

    get catalogs(): FilterCatalogItem[] {
        return this._catalogs;
    }

    set catalogs(value: FilterCatalogItem[]) {
        this._catalogs = value;
    }

    get pros(): FilterCatalogItem[] {
        return this._pros;
    }

    set pros(value: FilterCatalogItem[]) {
        this._pros = value;
    }

    get consumables(): FilterCatalogItem[] {
        return this._consumables;
    }

    set consumables(value: FilterCatalogItem[]) {
        this._consumables = value;
    }
}
