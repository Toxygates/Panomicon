import { IAttribute, Sample } from "src/app/shared/models/backend-types.model";
import { SampleFilter } from "src/app/shared/models/sample-filter.model";

export class SampleTableHelper {
  filters: SampleFilter[] = [];
  filteredSamples: Sample[] | undefined;

  clearFilters(): void {
    this.filters = [];
    this.filteredSamples = undefined;
  }

  formatterForFilters(filters: SampleFilter[]): Tabulator.Formatter {
    return (cell: Tabulator.CellComponent, _formatterParams: unknown,
        _onRendered: unknown) => {
      const value = cell.getValue() as string;
      const color = filters.every(filter =>
        filter.passesFilter(value)) ?
          "blue" : "red";
      return `<span style="color:${color}">${value}</span>`;
    }
  }

  createColumnForAttribute(attribute: IAttribute): Tabulator.ColumnDefinition {
    const column: Tabulator.ColumnDefinition =  {
      title: attribute.title,
      field: attribute.id,
    };
    const filtersForColumn = this.filters.filter(filter => filter.attribute == attribute.id);
    if (filtersForColumn.length > 0) {
      column.formatter =  this.formatterForFilters(filtersForColumn);
    }
    return column;
  }

  updateColumnFormatters(tabulator: Tabulator | undefined): void {
    const columns = tabulator?.getColumns()
    columns?.forEach(column => {
      const definition = column.getDefinition();
      const filtersForColumn = this.filters.filter(filter => filter.attribute == definition.field);
      const formatter = filtersForColumn.length > 0 ?
        this.formatterForFilters(filtersForColumn) :
        "plaintext";
      void column.updateDefinition({ title: definition.title, formatter: formatter} as unknown as Tabulator.ColumnDefinition);
    })
  }

  static groupHeader(selectedGroups: Set<string>): (value: string, count: number, _data: unknown,
      group: Tabulator.GroupComponent) => string {
    return (value: string, count: number, _data: unknown,
        group: Tabulator.GroupComponent) => {

      let prefix: string, itemCount: number, itemWord: string, button: string;

      if (group.getParentGroup()) {
        itemCount = count;
        itemWord = " sample";
        if (value != (group.getParentGroup() as Tabulator.GroupComponent).getKey()) {
          prefix = "Treatment group - ";
          if (selectedGroups.has(value)) {
            button = "<button type='button' class='btn btn-success'>"
              + "Group selected <i class='bi bi-check'></i></button>"
          } else {
            button = "<button type='button' class='btn btn-secondary'>"
            + "Select group</button>"
          }
        } else {
          prefix = "Control group - ";
          button = "";
        }
      } else {
        prefix = "Control group - ";
        itemCount = group.getSubGroups().length;
        itemWord = " group";
        button = "";
      }

      itemWord += itemCount != 1 ? "s" : "";

      return `${prefix}${value}<span>(${itemCount}${itemWord})</span> ${button}`;
    }
  }

  filterSamples(samples: Sample[], grouped: boolean): Sample[] {
    this.filteredSamples = samples.filter(sample =>
    this.filters.every(filter => filter.attribute && filter.passesFilter(sample[filter.attribute])));

    let samplesToReturn: Sample[];
    if (!grouped) {
      samplesToReturn = this.filteredSamples;
    } else {
      const includedTreatments = new Set<string>();
      this.filteredSamples.forEach(sample => {
        includedTreatments.add(sample.treatment);
        includedTreatments.add(sample.control_treatment);
      });
      const groupedFilteredSamples = samples.filter(sample =>
        includedTreatments.has(sample.treatment)
      );
      samplesToReturn = groupedFilteredSamples;
    }
    return samplesToReturn;
  }
}
