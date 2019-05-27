import React from 'react';
import { shallow } from 'enzyme';
import { Hello } from './Hello';
describe('First React component test with Enzyme', () => {
  it('renders without crashing', () => {
    shallow(<Hello />);
  });
});