export class Filter {
    name: string;
    value: string;
}

export class Filters {
    all: Filter[];

    constructor() {
        this.all = [];
    }
}