export class SampleFilter {
    attribute: string;
    type: SampleFilterType;
    argument: string;
    //numericalArgument: number = undefined;

    constructor() {}

    passesFilter(testValue: string): boolean {
        switch(this.type) {
            case(SampleFilterType.LessThan):
                return Number(testValue) < parseFloat(this.argument);
            case(SampleFilterType.GreaterThan):
                return Number(testValue) > parseFloat(this.argument);
            case(SampleFilterType.LessThanOrEqualTo):
                return Number(testValue) <= parseInt(this.argument);
            case(SampleFilterType.GreaterThanOrEqualTo):
                return Number(testValue) >= parseInt(this.argument);
            case(SampleFilterType.EqualTo):
                return Number(testValue) == parseInt(this.argument);
            case(SampleFilterType.NotEqualTo):
                return Number(testValue) != parseInt(this.argument);
            case(SampleFilterType.Contains):
                return testValue.includes(this.argument);
            case(SampleFilterType.DoesNotContain):
                return !testValue.includes(this.argument);
            case(SampleFilterType.AlphabeticallyBefore):
                return testValue.toLowerCase() <= this.argument.toLowerCase();
            case(SampleFilterType.AlphabeticallyAfter):
                return testValue.toLowerCase() >= this.argument.toLowerCase();
            default:
                return false;
        }
    }

    validate(): boolean {
        switch(this.type) {
            case(SampleFilterType.LessThan):
            case(SampleFilterType.GreaterThan):
                return !isNaN(Number(this.argument));
            case(SampleFilterType.LessThanOrEqualTo):
            case(SampleFilterType.GreaterThanOrEqualTo):
            case(SampleFilterType.EqualTo):
            case(SampleFilterType.NotEqualTo):
                return Number(this.argument) == parseInt(this.argument);
            case(SampleFilterType.Contains):
            case(SampleFilterType.DoesNotContain):
            case(SampleFilterType.AlphabeticallyBefore):
            case(SampleFilterType.AlphabeticallyAfter):
                return true;
        }
    }
}

export enum SampleFilterType {
    LessThan = "<",
    GreaterThan = ">",
    LessThanOrEqualTo = "<=",
    GreaterThanOrEqualTo = ">=",
    EqualTo = "=",
    NotEqualTo = "!=",
    Contains = "contains",
    DoesNotContain = "does not contain",
    AlphabeticallyBefore = "is alphabetically before (including)",
    AlphabeticallyAfter = "is alphabetically after (including)",
}
