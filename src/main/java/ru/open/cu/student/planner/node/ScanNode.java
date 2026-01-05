package ru.open.cu.student.planner.node;

import ru.open.cu.student.catalog.model.TableDefinition;

import java.util.List;

/**
 * Логический узел SeqScan — полное сканирование таблицы.
 * Выходную схему (имена колонок) получаем снаружи (из каталога).
 */
public class ScanNode extends LogicalPlanNode {

    private final TableDefinition tableDefinition;

    public ScanNode(TableDefinition tableDefinition, List<String> outputColumns) {
        super("SeqScan");
        this.tableDefinition = tableDefinition;
        this.outputColumns = List.copyOf(outputColumns);
    }

    public TableDefinition getTableDefinition() { return tableDefinition; }

    @Override
    public String prettyPrint(String indent) {
        return indent + "SeqScan(" + tableDefinition.name + ")\n";
    }
}
