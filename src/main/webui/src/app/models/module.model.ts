export class ModuleList {
    modules: Module[];
}

export class Module {
    name: string;
    description: string;
    primaryTags: string[];
    secondaryTags: string[];
    application: string;
    deployed: boolean;
    deleting: boolean;
    status: string;
    health: string;
    isDefault: boolean;
}

