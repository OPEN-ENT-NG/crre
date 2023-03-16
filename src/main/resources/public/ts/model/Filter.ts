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

export class FilterFront {
    name: string;
    value: string[];
}

export class FiltersFront {
    all: FilterFront[];

    constructor() {
        this.all = [];
    }
}

export interface IFilter {
    getValue(): string
}