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
    public disciplines: FilterCatalogItem[];
    public grades: FilterCatalogItem[];
    public classes: FilterCatalogItem[];
    public devices: FilterCatalogItem[];
    public targets: FilterCatalogItem[];
    public editors: FilterCatalogItem[];
    public distributors: FilterCatalogItem[];
    public catalogs: FilterCatalogItem[];
    public pros: FilterCatalogItem[];
    public consumables: FilterCatalogItem[];

    constructor() {
        this.disciplines = [];
        this.grades = [];
        this.classes = [];
        this.devices = [];
        this.targets = [];
        this.editors = [];
        this.distributors = [];
        this.catalogs = [];
        this.pros = [];
        this.consumables = [];
    }

    public build(filters: IFiltersCatalogItem): FiltersCatalogItem {
        this.disciplines = filters.disciplines.map((discipline: string) => {
            const item = new FilterCatalogItem(discipline);
            item.toString = () => item.name;
            item.nameFormat = item.name.replace("É", "E");
            return item;
        });
        this.grades = filters.grades.map((grade: string) => {
            const item = new FilterCatalogItem(grade);
            item.toString = () => item.name;
            return item;
        });
        this.classes = filters.classes.map((c: string) => {
            const item = new FilterCatalogItem(c);
            item.toString = () => item.name;
            return item;
        });
        this.devices = filters.devices.map((device: string) => {
            const item = new FilterCatalogItem(device);
            item.toString = () => item.name;
            return item;
        });
        this.targets = filters.targets.map((target: string) => {
            const item = new FilterCatalogItem(target);
            item.toString = () => item.name;
            return item;
        });
        this.editors = filters.editors.map((editor: string) => {
            const item = new FilterCatalogItem(editor);
            item.toString = () => item.name;
            item.nameFormat = item.name.replace("L’é", "e");
            return item;
        });
        this.distributors = filters.distributors.map((distributor: string) => {
            const item = new FilterCatalogItem(distributor);
            item.toString = () => item.name;
            return item;
        });

        this.catalogs = filters.catalogs.map((catalog: string) => {
            const item = new FilterCatalogItem(catalog);
            item.toString = () => lang.translate(item.name);
            return item;
        });

        return this;
    }
}
