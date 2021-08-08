import { ForbiddenValueListValidatorDirective } from './forbidden-value-list-validator.directive';

describe('ExclusionListValidatorDirective', () => {
  it('should create an instance', () => {
    const directive = new ForbiddenValueListValidatorDirective();
    void expect(directive).toBeTruthy();
  });
});
