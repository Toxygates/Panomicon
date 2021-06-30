import { IAttribute } from "./backend-types.model";

export class SampleFilter {
  attribute: string | undefined;
  type: SampleFilterType | undefined;
  parameter: string | undefined;

  passesFilter(testValue: string): boolean {
    if (!this.parameter) throw new Error("filter parameter is not defined");
    switch (this.type) {
      case (SampleFilterType.LessThan):
        return Number(testValue) < parseFloat(this.parameter);
      case (SampleFilterType.GreaterThan):
        return Number(testValue) > parseFloat(this.parameter);
      case (SampleFilterType.LessThanOrEqualTo):
        return Number(testValue) <= parseInt(this.parameter);
      case (SampleFilterType.GreaterThanOrEqualTo):
        return Number(testValue) >= parseInt(this.parameter);
      case (SampleFilterType.EqualTo):
        return Number(testValue) == parseInt(this.parameter);
      case (SampleFilterType.NotEqualTo):
        return Number(testValue) != parseInt(this.parameter);
      case (SampleFilterType.Contains):
        return testValue.includes(this.parameter);
      case (SampleFilterType.DoesNotContain):
        return !testValue.includes(this.parameter);
      case (SampleFilterType.AlphabeticallyBefore):
        return testValue.toLowerCase() <= this.parameter.toLowerCase();
      case (SampleFilterType.AlphabeticallyAfter):
        return testValue.toLowerCase() >= this.parameter.toLowerCase();
      default:
        return false;
    }
  }

  validate(attributeMap: Map<string, IAttribute>): boolean {
    return this.validateAttribute(attributeMap) &&
      this.validateType(attributeMap) &&
      this.validateParameter();
  }

  validateAttribute(attributeMap: Map<string, IAttribute>): boolean {
    return this.attribute != null && (attributeMap.has(this.attribute));
  }

  validateType(attributeMap: Map<string, IAttribute>,
      type: SampleFilterType | undefined = this.type): boolean {
    const foundAttribute =
      this.attribute != null && attributeMap.get(this.attribute);
    if (foundAttribute) {
      switch (type) {
        case (SampleFilterType.LessThan):
        case (SampleFilterType.GreaterThan):
        case (SampleFilterType.LessThanOrEqualTo):
        case (SampleFilterType.GreaterThanOrEqualTo):
        case (SampleFilterType.EqualTo):
        case (SampleFilterType.NotEqualTo):
          return foundAttribute.isNumerical;
        case (SampleFilterType.Contains):
        case (SampleFilterType.DoesNotContain):
        case (SampleFilterType.AlphabeticallyBefore):
        case (SampleFilterType.AlphabeticallyAfter):
          return true;
        default:
          return false;
      }
    } else {
      return type != null;
    }
  }

  validateParameter(): boolean {
    switch (this.type) {
      case (SampleFilterType.LessThan):
      case (SampleFilterType.GreaterThan):
        return !isNaN(Number(this.parameter)) && this.parameter != "";
      case (SampleFilterType.LessThanOrEqualTo):
      case (SampleFilterType.GreaterThanOrEqualTo):
      case (SampleFilterType.EqualTo):
      case (SampleFilterType.NotEqualTo):
        return (this.parameter != undefined) && Number(this.parameter) == parseInt(this.parameter);
      case (SampleFilterType.Contains):
      case (SampleFilterType.DoesNotContain):
      case (SampleFilterType.AlphabeticallyBefore):
      case (SampleFilterType.AlphabeticallyAfter):
      default:
        return this.parameter != undefined && this.parameter != "";
    }
  }

  correctTypeInfo(attributeMap: Map<string, IAttribute>): string {
    const foundAttribute =
      this.attribute != null && attributeMap.get(this.attribute);
    if (foundAttribute && !foundAttribute.isNumerical) {
      switch (this.type) {
        case (SampleFilterType.LessThan):
        case (SampleFilterType.GreaterThan):
        case (SampleFilterType.LessThanOrEqualTo):
        case (SampleFilterType.GreaterThanOrEqualTo):
        case (SampleFilterType.EqualTo):
        case (SampleFilterType.NotEqualTo):
          return "a non-numerical filter type"
      }
    }
    return "a filter type";
  }

  correctParameterInfo(): string {
    switch (this.type) {
      case (SampleFilterType.LessThan):
      case (SampleFilterType.GreaterThan):
        return "a number"
      case (SampleFilterType.LessThanOrEqualTo):
      case (SampleFilterType.GreaterThanOrEqualTo):
      case (SampleFilterType.EqualTo):
      case (SampleFilterType.NotEqualTo):
        return "an integer"
      case (SampleFilterType.Contains):
      case (SampleFilterType.DoesNotContain):
      case (SampleFilterType.AlphabeticallyBefore):
      case (SampleFilterType.AlphabeticallyAfter):
      default:
        return "a string";
    }
  }

  clone(): SampleFilter {
    const clone = new SampleFilter();
    clone.attribute = this.attribute;
    clone.type = this.type;
    clone.parameter = this.parameter;
    return clone;
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
