import { BehaviorSubject } from "rxjs";
import { Attribute } from "../../shared/models/backend-types.model";
import { SampleFilter } from "../../shared/models/sample-filter.model";

export class SampleTableHelper {

  constructor(private filters$: BehaviorSubject<SampleFilter[]>) {}

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

  createColumnForAttribute(attribute: Attribute): Tabulator.ColumnDefinition {
    const column: Tabulator.ColumnDefinition =  {
      title: attribute.title,
      field: attribute.id,
    };
    const filtersForColumn = this.filters$.value.filter(filter => filter.attribute == attribute.id);
    if (filtersForColumn.length > 0) {
      column.formatter =  this.formatterForFilters(filtersForColumn);
    }
    return column;
  }

  updateColumnFormatters(tabulator: Tabulator | undefined): void {
    const columns = tabulator?.getColumns()
    columns?.forEach(column => {
      const definition = column.getDefinition();
      const filtersForColumn = this.filters$.value.filter(filter => filter.attribute == definition.field);
      const formatter = filtersForColumn.length > 0 ?
        this.formatterForFilters(filtersForColumn) :
        "plaintext";
      void column.updateDefinition({ title: definition.title, formatter: formatter} as Tabulator.ColumnDefinition);
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
}
