export class SampleFilter {
    attribute: string;
    type: SampleFilterType;
    parameter: string;

    passesFilter(testValue: string): boolean {
        switch(this.type) {
            case(SampleFilterType.LessThan):
                return Number(testValue) < parseFloat(this.parameter);
            case(SampleFilterType.GreaterThan):
                return Number(testValue) > parseFloat(this.parameter);
            case(SampleFilterType.LessThanOrEqualTo):
                return Number(testValue) <= parseInt(this.parameter);
            case(SampleFilterType.GreaterThanOrEqualTo):
                return Number(testValue) >= parseInt(this.parameter);
            case(SampleFilterType.EqualTo):
                return Number(testValue) == parseInt(this.parameter);
            case(SampleFilterType.NotEqualTo):
                return Number(testValue) != parseInt(this.parameter);
            case(SampleFilterType.Contains):
                return testValue.includes(this.parameter);
            case(SampleFilterType.DoesNotContain):
                return !testValue.includes(this.parameter);
            case(SampleFilterType.AlphabeticallyBefore):
                return testValue.toLowerCase() <= this.parameter.toLowerCase();
            case(SampleFilterType.AlphabeticallyAfter):
                return testValue.toLowerCase() >= this.parameter.toLowerCase();
            default:
                return false;
        }
    }

    validate(): boolean {
        return this.validateAttribute() && this.validateType() &&
            this.validateParameter();
    }

    validateAttribute(): boolean {
        return this.attribute != null;
    }

    validateType(): boolean {
        return this.type != null;
    }

    validateParameter(): boolean {
        switch(this.type) {
            case(SampleFilterType.LessThan):
            case(SampleFilterType.GreaterThan):
                return !isNaN(Number(this.parameter)) && this.parameter != "";
            case(SampleFilterType.LessThanOrEqualTo):
            case(SampleFilterType.GreaterThanOrEqualTo):
            case(SampleFilterType.EqualTo):
            case(SampleFilterType.NotEqualTo):
                return Number(this.parameter) == parseInt(this.parameter);
            case(SampleFilterType.Contains):
            case(SampleFilterType.DoesNotContain):
            case(SampleFilterType.AlphabeticallyBefore):
            case(SampleFilterType.AlphabeticallyAfter):
            default:
                return this.parameter != undefined && this.parameter != "";
        }
    }

    correctParameterInfo(): string {
        switch(this.type) {
            case(SampleFilterType.LessThan):
            case(SampleFilterType.GreaterThan):
                return "a number"
            case(SampleFilterType.LessThanOrEqualTo):
            case(SampleFilterType.GreaterThanOrEqualTo):
            case(SampleFilterType.EqualTo):
            case(SampleFilterType.NotEqualTo):
                return "an integer"
            case(SampleFilterType.Contains):
            case(SampleFilterType.DoesNotContain):
            case(SampleFilterType.AlphabeticallyBefore):
            case(SampleFilterType.AlphabeticallyAfter):
            default:
                return "a string";
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
    AlphabeticallyBefore = "is alphabetically before (inclusive)",
    AlphabeticallyAfter = "is alphabetically after (inclusive)",
}
