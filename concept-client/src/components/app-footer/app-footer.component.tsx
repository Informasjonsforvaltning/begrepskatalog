import React, { FunctionComponent } from 'react';
import './app-footer.scss';

import { localization } from '../../lib/localization';

export const FooterPure: FunctionComponent = (): JSX.Element => (
  <footer className="fdk-footer">
    <div className="container">
      <div className="row">
        <div className="col-md-4">
          <p className="fdk-p-footer">
            <a href="https://www.brreg.no/personvernerklaering/">
              {localization['information']}
              {localization['privacy']}
              <i className="fa fa-external-link fdk-fa-right" />
            </a>
          </p>
        </div>
        <div className="col-md-4 text-center">
          <span className="uu-invisible" aria-hidden="false">
            Felles Datakatalog.
          </span>
          <p className="fdk-p-footer">{localization['informationText']}</p>
        </div>
        <div className="col-md-4 text-right">
          <p className="fdk-p-footer">
            <a href="mailto:fellesdatakatalog@brreg.no">
              <span className="uu-invisible" aria-hidden="false">
                Mailadresse.
              </span>
              {localization['mail']}
            </a>
          </p>
        </div>
      </div>
    </div>
  </footer>
);

export const Footer = FooterPure;
