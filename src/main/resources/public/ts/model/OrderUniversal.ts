import {Campaign} from "./Campaign";
import {Basket} from "./Basket";
import {Project} from "./Project";

export class OrderUniversal {
    private _id: number;
    private _amount: number;
    private _prescriber_validation_date: Date;
    private _campaign: Campaign;
    private _id_structure: string;
    private _status: string;
    private _equipment_key: string;
    private _cause_status: string;
    private _comment: string;
    private _prescriber_id: string;
    private _basket: Basket;
    private _reassort: boolean;
    private _validator_id: string;
    private _validator_name: string;
    private _validator_validation_date: Date;
    private _modification_date: string;
    private _project: Project;
    private _equipment_name: string;
    private _equipment_image: string;
    private _equipment_price: number;
    private _equipment_grade: string;
    private _equipment_editor: string;
    private _equipment_diffusor: string;
    private _equipment_format: string;
    private _equipment_tva5: number;
    private _equipment_tva20: number;
    private _equipment_priceht: number;
    private _offers: any[];
    private _totalFree: number;
    private _valid: boolean;

    private _order_client_id: number;
    private _order_region_id: number;


    get id(): number {
        return this._id;
    }

    set id(value: number) {
        this._id = value;
    }

    get amount(): number {
        return this._amount;
    }

    set amount(value: number) {
        this._amount = value;
    }

    get prescriber_validation_date(): Date {
        return this._prescriber_validation_date;
    }

    set prescriber_validation_date(value: Date) {
        this._prescriber_validation_date = value;
    }

    get campaign(): Campaign {
        return this._campaign;
    }

    set campaign(value: Campaign) {
        this._campaign = value;
    }

    get id_structure(): string {
        return this._id_structure;
    }

    set id_structure(value: string) {
        this._id_structure = value;
    }

    get status(): string {
        return this._status;
    }

    set status(value: string) {
        this._status = value;
    }

    get equipment_key(): string {
        return this._equipment_key;
    }

    set equipment_key(value: string) {
        this._equipment_key = value;
    }

    get cause_status(): string {
        return this._cause_status;
    }

    set cause_status(value: string) {
        this._cause_status = value;
    }

    get comment(): string {
        return this._comment;
    }

    set comment(value: string) {
        this._comment = value;
    }

    get prescriber_id(): string {
        return this._prescriber_id;
    }

    set prescriber_id(value: string) {
        this._prescriber_id = value;
    }

    get basket(): Basket {
        return this._basket;
    }

    set basket(value: Basket) {
        this._basket = value;
    }

    get reassort(): boolean {
        return this._reassort;
    }

    set reassort(value: boolean) {
        this._reassort = value;
    }

    get validator_id(): string {
        return this._validator_id;
    }

    set validator_id(value: string) {
        this._validator_id = value;
    }

    get validator_name(): string {
        return this._validator_name;
    }

    set validator_name(value: string) {
        this._validator_name = value;
    }

    get validator_validation_date(): Date {
        return this._validator_validation_date;
    }

    set validator_validation_date(value: Date) {
        this._validator_validation_date = value;
    }

    get modification_date(): string {
        return this._modification_date;
    }

    set modification_date(value: string) {
        this._modification_date = value;
    }

    get project(): Project {
        return this._project;
    }

    set project(value: Project) {
        this._project = value;
    }

    get equipment_name(): string {
        return this._equipment_name;
    }

    set equipment_name(value: string) {
        this._equipment_name = value;
    }

    get equipment_image(): string {
        return this._equipment_image;
    }

    set equipment_image(value: string) {
        this._equipment_image = value;
    }

    get equipment_price(): number {
        return this._equipment_price;
    }

    set equipment_price(value: number) {
        this._equipment_price = value;
    }

    get equipment_grade(): string {
        return this._equipment_grade;
    }

    set equipment_grade(value: string) {
        this._equipment_grade = value;
    }

    get equipment_editor(): string {
        return this._equipment_editor;
    }

    set equipment_editor(value: string) {
        this._equipment_editor = value;
    }

    get equipment_diffusor(): string {
        return this._equipment_diffusor;
    }

    set equipment_diffusor(value: string) {
        this._equipment_diffusor = value;
    }

    get equipment_format(): string {
        return this._equipment_format;
    }

    set equipment_format(value: string) {
        this._equipment_format = value;
    }

    get equipment_tva5(): number {
        return this._equipment_tva5;
    }

    set equipment_tva5(value: number) {
        this._equipment_tva5 = value;
    }

    get equipment_tva20(): number {
        return this._equipment_tva20;
    }

    set equipment_tva20(value: number) {
        this._equipment_tva20 = value;
    }

    get equipment_priceht(): number {
        return this._equipment_priceht;
    }

    set equipment_priceht(value: number) {
        this._equipment_priceht = value;
    }

    get offers(): any[] {
        return this._offers;
    }

    set offers(value: any[]) {
        this._offers = value;
    }

    get totalFree(): number {
        return this._totalFree;
    }

    set totalFree(value: number) {
        this._totalFree = value;
    }


    get order_client_id(): number {
        return this._order_client_id;
    }

    set order_client_id(value: number) {
        this._order_client_id = value;
    }

    get order_region_id(): number {
        return this._order_region_id;
    }

    set order_region_id(value: number) {
        this._order_region_id = value;
    }

    get valid(): boolean {
        return this._valid;
    }

    set valid(value: boolean) {
        this._valid = value;
    }
}