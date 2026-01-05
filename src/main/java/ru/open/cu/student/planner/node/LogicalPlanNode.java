package ru.open.cu.student.planner.node;


import java.util.List;

/**
 * Абстрактная базовая нода логического плана.
 * Все логические операторы (Project, Filter, Scan, CreateTable, Insert)
 * наследуются от неё.
 */
public abstract class LogicalPlanNode {

    protected final String nodeType;       // тип узла (Project, Filter и т.д.)
    protected List<String> outputColumns;  // схема выходных колонок

    protected LogicalPlanNode(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getNodeType() {
        return nodeType;
    }

    public List<String> getOutputColumns() {
        return outputColumns;
    }

    public void setOutputColumns(List<String> outputColumns) {
        this.outputColumns = outputColumns;
    }

    /**
     * Удобный метод для отладки — красивый вывод дерева
     */
    public String prettyPrint(String indent) {
        return indent + nodeType + "\n";
    }

    @Override
    public String toString() {
        return nodeType;
    }
}
