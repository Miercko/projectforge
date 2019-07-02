import React from 'react';
import CreatableSelect from 'react-select/lib/Creatable';
import { DynamicLayoutContext } from '../../../components/base/dynamicLayout/context';
import ActionGroup from '../../../components/base/page/action/Group';
import { Card, CardBody, Col, FormGroup, Label, Row } from '../../../components/design';
import EditableMultiValueLabel from '../../../components/design/EditableMultiValueLabel';
import ReactSelect from '../../../components/design/ReactSelect';
import { getNamedContainer } from '../../../utilities/layout';
import FavoritesPanel from '../../panel/FavoritesPanel';


function SearchFilter() {
    const {
        filter,
        ui,
        renderLayout,
        setFilter,
    } = React.useContext(DynamicLayoutContext);

    const handleFavoriteCreate = id => console.log(id);
    const handleFavoriteDelete = id => console.log(id);
    const handleFavoriteSelect = id => console.log(id);
    const handleFavoriteRename = (id, newName) => console.log(id, newName);
    const handleFavoriteUpdate = id => console.log(id);

    const handleMaxRowsChange = ({ value }) => setFilter({
        ...filter,
        maxRows: value,
    });

    const handleSearchFilterChange = (id, newValue) => setFilter(stateFilter => ({
        ...stateFilter,
        [id]: newValue,
    }));

    const searchFilter = getNamedContainer('searchFilter', ui.namedContainers);

    console.log(filter);

    return (
        <Card>
            <CardBody>
                <form>
                    <Row>
                        <Col sm={11}>
                            <CreatableSelect
                                components={{
                                    MultiValueLabel: EditableMultiValueLabel,
                                }}
                                isClearable
                                isMulti
                                name="searchFilter"
                                options={searchFilter ? searchFilter.content.map(option => ({
                                    ...option,
                                    value: option.key,
                                    // TODO DISPLAYED LABEL CAN BE SET HERE
                                    label: option.id,
                                })) : []}
                                setMultiValue={handleSearchFilterChange}
                                values={filter}
                            />
                        </Col>
                        <Col sm={1} className="d-flex align-items-center">
                            <FavoritesPanel
                                onFavoriteCreate={handleFavoriteCreate}
                                onFavoriteDelete={handleFavoriteDelete}
                                onFavoriteRename={handleFavoriteRename}
                                onFavoriteSelect={handleFavoriteSelect}
                                onFavoriteUpdate={handleFavoriteUpdate}
                                translations={ui.translations}
                            />
                        </Col>
                    </Row>
                    <Row>
                        <Col sm={8}>
                            <FormGroup row>
                                <Label sm={2}>[Optionen]</Label>
                                <Col sm={10}>
                                    {renderLayout((getNamedContainer('filterOptions', ui.namedContainers) || {}).content)}
                                </Col>
                            </FormGroup>
                        </Col>
                        <Col sm={4}>
                            <ReactSelect
                                label="[Seitengröße]"
                                onChange={handleMaxRowsChange}
                                required
                                translations={ui.translations}
                                value={{
                                    value: filter.maxRows,
                                    label: filter.maxRows,
                                }}
                                values={[
                                    {
                                        value: 25,
                                        label: '25',
                                    },
                                    {
                                        value: 50,
                                        label: '50',
                                    },
                                    {
                                        value: 100,
                                        label: '100',
                                    },
                                    {
                                        value: 200,
                                        label: '200',
                                    },
                                    {
                                        value: 500,
                                        label: '500',
                                    },
                                    {
                                        value: 1000,
                                        label: '1000',
                                    },
                                ]}
                            />
                        </Col>
                    </Row>
                    <FormGroup row>
                        <Col>
                            <ActionGroup actions={ui.actions} />
                        </Col>
                    </FormGroup>
                </form>
            </CardBody>
        </Card>
    );
}

SearchFilter.propTypes = {};

SearchFilter.defaultProps = {};

export default SearchFilter;
