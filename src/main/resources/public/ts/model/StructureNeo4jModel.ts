export class StructureNeo4jModel {
    private _id: string;
    private _uai: string;
    private _name: string;
    private _phone: string;
    private _address: string;
    private _zip_code: string;
    private _type: string;
    private _city: string;


    get id(): string {
        return this._id;
    }

    set id(value: string) {
        this._id = value;
    }

    get uai(): string {
        return this._uai;
    }

    set uai(value: string) {
        this._uai = value;
    }

    get name(): string {
        return this._name;
    }

    set name(value: string) {
        this._name = value;
    }

    get phone(): string {
        return this._phone;
    }

    set phone(value: string) {
        this._phone = value;
    }

    get address(): string {
        return this._address;
    }

    set address(value: string) {
        this._address = value;
    }

    get zip_code(): string {
        return this._zip_code;
    }

    set zip_code(value: string) {
        this._zip_code = value;
    }

    get type(): string {
        return this._type;
    }

    set type(value: string) {
        this._type = value;
    }

    get city(): string {
        return this._city;
    }

    set city(value: string) {
        this._city = value;
    }
}