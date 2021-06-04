import { SampleFilter, SampleFilterType } from './sample-filter.model';

describe('SampleFilter', () => {
  let filter: SampleFilter;

  beforeEach(() => {
    filter = new SampleFilter();
    filter.attribute = "some-attribute";
    filter.argument = "23";
  });

  it('should correctly test value < argument', () => {
    filter.type = SampleFilterType.LessThan;
    expect(filter.passesFilter("22.2")).toBeTrue();
    expect(filter.passesFilter("23.4")).toBeFalse();
    expect(filter.passesFilter("23")).toBeFalse();
  });

  it('should correctly test value > argument', () => {
    filter.type = SampleFilterType.GreaterThan;
    expect(filter.passesFilter("22.2")).toBeFalse();
    expect(filter.passesFilter("23.4")).toBeTrue();
    expect(filter.passesFilter("23")).toBeFalse();
  });

  it('should correctly test value <= argument', () => {
    filter.type = SampleFilterType.LessThanOrEqualTo;
    expect(filter.passesFilter("22.2")).toBeTrue();
    expect(filter.passesFilter("23.4")).toBeFalse();
    expect(filter.passesFilter("23")).toBeTrue();
  });

  it('should correctly test value >= argument', () => {
    filter.type = SampleFilterType.GreaterThanOrEqualTo;
    expect(filter.passesFilter("22.2")).toBeFalse();
    expect(filter.passesFilter("23.4")).toBeTrue();
    expect(filter.passesFilter("23")).toBeTrue();
  });

  it('should correctly test value == argument', () => {
    filter.type = SampleFilterType.EqualTo;
    expect(filter.passesFilter("22.2")).toBeFalse();
    expect(filter.passesFilter("23.4")).toBeFalse();
    expect(filter.passesFilter("23")).toBeTrue();
  });

  it('should correctly test value != argument', () => {
    filter.type = SampleFilterType.NotEqualTo;
    expect(filter.passesFilter("22.2")).toBeTrue();
    expect(filter.passesFilter("23.4")).toBeTrue();
    expect(filter.passesFilter("23")).toBeFalse();
  });

  it('should correctly test value contains argument', () => {
    filter.type = SampleFilterType.Contains;
    expect(filter.passesFilter("there is a 23 here")).toBeTrue();
    expect(filter.passesFilter("23")).toBeTrue();
    expect(filter.passesFilter("2 and a 3 here but not the other thing")).toBeFalse();
  });

  it('should correctly test value does not contain argument', () => {
    filter.type = SampleFilterType.DoesNotContain;
    expect(filter.passesFilter("there is a 23 here")).toBeFalse();
    expect(filter.passesFilter("23")).toBeFalse();
    expect(filter.passesFilter("2 and a 3 here but not the other thing")).toBeTrue();
  });

  it('should correctly test value alphabetically before argument', () => {
    filter.type = SampleFilterType.AlphabeticallyBefore
    filter.argument = "blah";
    expect(filter.passesFilter("abracadabra")).toBeTrue();
    expect(filter.passesFilter("blahg")).toBeFalse();
    expect(filter.passesFilter("blah")).toBeTrue();
  });

  it('should correctly test value alphabetically after argument', () => {
    filter.type = SampleFilterType.AlphabeticallyAfter
    filter.argument = "blah";
    expect(filter.passesFilter("abracadabra")).toBeFalse();
    expect(filter.passesFilter("blahg")).toBeTrue();
    expect(filter.passesFilter("blah")).toBeTrue();
  });

  it('should only validate numbers for < and >', () => {
    [SampleFilterType.LessThan, SampleFilterType.GreaterThan].forEach(type => {
      filter.type = type;
      filter.argument = undefined;
      expect(filter.validate()).toBeFalse();
      filter.argument = "";
      expect(filter.validate()).toBeFalse();
      filter.argument = "blah";
      expect(filter.validate()).toBeFalse();
      filter.argument = "23";
      expect(filter.validate()).toBeTrue();
    })
  });

  it('should only validate integers for <=, >=, ==, and !=', () => {
    [SampleFilterType.LessThanOrEqualTo, SampleFilterType.GreaterThanOrEqualTo,
     SampleFilterType.EqualTo, SampleFilterType.NotEqualTo].forEach(type => {
      filter.type = type;
      filter.argument = undefined;
      expect(filter.validate()).toBeFalse();
      filter.argument = "";
      expect(filter.validate()).toBeFalse();
      filter.argument = "blah";
      expect(filter.validate()).toBeFalse();
      filter.argument = "23";
      expect(filter.validate()).toBeTrue();
      filter.argument = "23.2";
      expect(filter.validate()).toBeFalse();
    })
  });

  it('should validate numeric and non-numeric strings for contains, does not contain, and alphabetically before/after', () => {
    [SampleFilterType.Contains, SampleFilterType.DoesNotContain,
     SampleFilterType.AlphabeticallyBefore, SampleFilterType.AlphabeticallyAfter].forEach(type => {
      filter.type = type;
      filter.argument = undefined;
      expect(filter.validate()).toBeFalse();
      filter.argument = "";
      expect(filter.validate()).toBeFalse();
      filter.argument = "blah";
      expect(filter.validate()).toBeTrue();
      filter.argument = "23.5";
      expect(filter.validate()).toBeTrue();
    })
  });
});
