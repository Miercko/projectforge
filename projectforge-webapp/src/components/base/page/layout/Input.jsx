import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { changeEditFormField } from '../../../../actions';
import revisedRandomId from '../../../../utilities/revisedRandomId';
import { Col, Input } from '../../../design';
import style from '../Page.module.scss';

class LayoutInput extends Component {
    constructor(props) {
        super(props);

        this.handleInputChange = this.handleInputChange.bind(this);
    }

    handleInputChange(event) {
        const { id, changeValue, type } = this.props;

        if (!id) {
            return;
        }

        let newValue;

        switch (type) {
            case 'checkbox':
                newValue = event.target.checked;
                break;
            default:
                newValue = event.target.value;
        }

        changeValue(id, newValue);
    }

    render() {
        const {
            id, type, values, formFieldValues,
        } = this.props;

        // TODO: VALIDATION

        let children;
        let ColTag = Col;
        const inputProps = {};

        if (type === 'select') {
            children = values.map(option => (
                <option
                    value={option.value}
                    key={`input-select-value-${revisedRandomId()}`}
                >
                    {option.title}
                </option>
            ));

            ColTag = React.Fragment;
            inputProps.className = style.select;
        }

        if (type === 'checkbox') {
            inputProps.checked = formFieldValues[id] || false;
        } else {
            inputProps.value = formFieldValues[id] || '';
        }

        return (
            <ColTag>
                <Input
                    type={type}
                    name={id}
                    id={id}
                    {...inputProps}
                    onChange={this.handleInputChange}
                >
                    {children}
                </Input>
            </ColTag>
        );
    }
}

LayoutInput.propTypes = {
    changeValue: PropTypes.func.isRequired,
    formFieldValues: PropTypes.objectOf(PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
        PropTypes.bool,
        PropTypes.objectOf(PropTypes.oneOfType([
            PropTypes.string,
            PropTypes.number,
            PropTypes.bool,
        ])),
    ])).isRequired,
    id: PropTypes.string,
    type: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.shape({
        value: PropTypes.string,
        title: PropTypes.string,
    })),
};

LayoutInput.defaultProps = {
    id: undefined,
    type: 'text',
    values: [],
};

const mapStateToProps = state => ({
    formFieldValues: state.editPage.values,
});

const actions = {
    changeValue: changeEditFormField,
};

export default connect(mapStateToProps, actions)(LayoutInput);