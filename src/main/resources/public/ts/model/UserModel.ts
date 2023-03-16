import {IFilter} from "./Filter";

export interface IUserModel extends IFilter{
    id_user: string;
    user_name: string;
}

export class UserModel implements IUserModel {
    private _id_user: string;
    private _user_name: string;

    constructor(element: IUserModel) {
        this._id_user = element.id_user;
        this._user_name = element.user_name;
    }

    get id_user(): string {
        return this._id_user;
    }

    set id_user(value: string) {
        this._id_user = value;
    }

    get user_name(): string {
        return this._user_name;
    }

    set user_name(value: string) {
        this._user_name = value;
    }

    toString(): string {
        return this._user_name;
    }

    getValue(): string {
        return this._id_user;
    }
}