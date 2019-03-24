import PropTypes from 'prop-types';
import React from 'react';
import { Label } from '../../../design';
import style from '../Page.module.scss';

function LayoutLabel({ label, for: forComponent }) {
    return <Label for={forComponent} className={style.inputLabel}>{label}</Label>;
}

LayoutLabel.propTypes = {
    value: PropTypes.string.isRequired,
    for: PropTypes.string,
};

LayoutLabel.defaultProps = {
    for: undefined,
};

export default LayoutLabel;
