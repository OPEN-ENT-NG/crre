export interface IWorkflowNeo4jModel {
    name: string
    display_name: string
}

export class WorkflowNeo4jModel implements IWorkflowNeo4jModel{
    private _name: string
    private _display_name: string

    constructor(data: IWorkflowNeo4jModel) {
        this._name = data.name;
        this._display_name = data.display_name;
    }

    get name(): string {
        return this._name;
    }
    set name(value: string) {
        this._name = value;
    }


    get display_name(): string {
        return this._display_name;
    }

    set display_name(value: string) {
        this._display_name = value;
    }
}