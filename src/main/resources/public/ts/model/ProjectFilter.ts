import {moment} from "entcore";
import {Campaign} from "./Campaign";
import {ORDER_STATUS_ENUM} from "../enum/order-status-enum";
import {Structure} from "./Structure";
import {StatusFilter} from "./StatusFilter";

export class ProjectFilter {
    private _startDate;
    private _endDate;
    private _queryName: string;
    private _filterChoiceCorrelation: Map<string, string>;
    private _campaignList: Array<Campaign>;
    private _statusFilterList: Array<StatusFilter>;
    private _structureList: Array<Structure>;
    private _catalogList: Array<{name}>;
    private _schoolType: Array<{name}>;
    private _editorList: Array<String>;
    private _distributorList: Array<String>;


    constructor() {
        this._startDate = moment().add(-6, 'months')._d;
        this._endDate = moment()._d;
        this._queryName = "";
        this._filterChoiceCorrelation = new Map<string, string>();
        this._campaignList = [];
        this._statusFilterList = [];
        this._structureList = [];
        this._catalogList = [];
        this._schoolType = [];
        this._editorList = [];
        this._distributorList = [];
    }

    get startDate() {
        return this._startDate;
    }

    set startDate(value) {
        this._startDate = value;
    }

    get endDate() {
        return this._endDate;
    }

    set endDate(value) {
        this._endDate = value;
    }

    get queryName(): string {
        return this._queryName;
    }

    set queryName(value: string) {
        this._queryName = value;
    }

    get filterChoiceCorrelation(): Map<string, string> {
        return this._filterChoiceCorrelation;
    }

    set filterChoiceCorrelation(value: Map<string, string>) {
        this._filterChoiceCorrelation = value;
    }

    get campaignList(): Array<Campaign> {
        return this._campaignList;
    }

    set campaignList(value: Array<Campaign>) {
        this._campaignList = value;
    }

    get statusFilterList(): Array<StatusFilter> {
        return this._statusFilterList;
    }

    set statusFilterList(value: Array<StatusFilter>) {
        this._statusFilterList = value;
    }

    get structureList(): Array<Structure> {
        return this._structureList;
    }

    set structureList(value: Array<Structure>) {
        this._structureList = value;
    }

    get catalogList(): Array<{ name }> {
        return this._catalogList;
    }

    set catalogList(value: Array<{ name }>) {
        this._catalogList = value;
    }

    get schoolType(): Array<{ name }> {
        return this._schoolType;
    }

    set schoolType(value: Array<{ name }>) {
        this._schoolType = value;
    }

    get editorList(): Array<String> {
        return this._editorList;
    }

    set editorList(value: Array<String>) {
        this._editorList = value;
    }

    get distributorList(): Array<String> {
        return this._distributorList;
    }

    set distributorList(value: Array<String>) {
        this._distributorList = value;
    }

    filterChoiceCorrelationKey(): Array<string> {
        return Array.from(this.filterChoiceCorrelation.keys());
    }
}