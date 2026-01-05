package ru.open.cu.student.optimizer.node;

import java.util.List;

/**
 * Абстрактный класс физического плана.
 * Каждый конкретный PhysicalPlanNode описывает, КАК будет выполняться операция:
 * - какой алгоритм сканирования (SeqScan, IndexScan, ...)
 * - как применяется фильтр (Filter)
 * - какие колонки выбираются (Project)
 */
public abstract class PhysicalPlanNode {

    private String nodeType;
    protected List<String> outputColumns; // схема выхода для узла (имена)

    protected PhysicalPlanNode(String nodeType) {
        this.nodeType = nodeType;
    }
    protected PhysicalPlanNode() {}

    public String getNodeType() {
        return nodeType;
    }

    public List<String> getOutputColumns() { return outputColumns; }
    public void setOutputColumns(List<String> cols) { this.outputColumns = cols; }

    /**
     * Рекурсивная печать дерева плана — по умолчанию только тип
     */
    public String prettyPrint(String indent) {
        return indent + nodeType + "\n";
    }

    @Override
    public String toString() {
        return nodeType;
    }
}
