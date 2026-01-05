package ru.open.cu.student.index.btree;

import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.index.TID;

import java.util.*;


public class BPlusTreeIndexImpl implements BPlusTreeIndex {

    private final String name;
    private final String tableName;
    private final String columnName;
    private final int order;

    private Node root;
    private final LeafNode leftmostLeaf;
    private int height = 1;

    public BPlusTreeIndexImpl(String name, String tableName, String columnName, int order) {
        if (order < 2) throw new IllegalArgumentException("order must be >= 2");
        this.name = name;
        this.tableName = tableName;
        this.columnName = columnName;
        this.order = order;
        this.root = new LeafNode();
        this.leftmostLeaf = (LeafNode) root;
    }

    public BPlusTreeIndexImpl(String name, String tableName, String columnName) {
        this(name, tableName, columnName, 4);
    }

    @Override
    public synchronized void insert(Comparable<?> key, TID tid) {
        if (key == null || tid == null) return;
        LeafNode leaf = findLeaf(key);
        leaf.insert(key, tid);
        if (leaf.isOverflow()) {
            splitLeaf(leaf);
        }
    }

    @Override
    public synchronized List<TID> search(Comparable<?> key) {
        if (key == null) return List.of();
        LeafNode leaf = findLeaf(key);
        return new ArrayList<>(leaf.get(key));
    }

    @Override
    public synchronized List<TID> rangeSearch(Comparable<?> from, boolean includeFrom,
                                              Comparable<?> to, boolean includeTo) {
        if (from == null || to == null) return List.of();
        if (compare(from, to) > 0) return List.of();

        LeafNode leaf = findLeaf(from);
        List<TID> result = new ArrayList<>();
        LeafNode current = leaf;
        while (current != null) {
            for (int i = 0; i < current.keys.size(); i++) {
                Comparable<?> key = current.keys.get(i);
                if (compare(key, from) < 0 || (!includeFrom && compare(key, from) == 0)) {
                    continue;
                }
                if (compare(key, to) > 0 || (!includeTo && compare(key, to) == 0)) {
                    return result;
                }
                result.addAll(current.values.get(i));
            }
            current = current.next;
        }
        return result;
    }

    @Override
    public synchronized List<TID> searchGreaterThan(Comparable<?> value, boolean inclusive) {
        if (value == null) return scanAll();
        LeafNode leaf = findLeaf(value);
        List<TID> result = new ArrayList<>();
        LeafNode current = leaf;
        while (current != null) {
            for (int i = 0; i < current.keys.size(); i++) {
                Comparable<?> key = current.keys.get(i);
                int cmp = compare(key, value);
                if (cmp > 0 || (inclusive && cmp == 0)) {
                    result.addAll(current.values.get(i));
                }
            }
            current = current.next;
        }
        return result;
    }

    @Override
    public synchronized List<TID> searchLessThan(Comparable<?> value, boolean inclusive) {
        if (value == null) return scanAll();
        List<TID> result = new ArrayList<>();
        LeafNode current = leftmostLeaf;
        while (current != null) {
            for (int i = 0; i < current.keys.size(); i++) {
                Comparable<?> key = current.keys.get(i);
                int cmp = compare(key, value);
                if (cmp < 0 || (inclusive && cmp == 0)) {
                    result.addAll(current.values.get(i));
                } else {
                    return result;
                }
            }
            current = current.next;
        }
        return result;
    }

    @Override
    public synchronized List<TID> scanAll() {
        List<TID> result = new ArrayList<>();
        LeafNode current = leftmostLeaf;
        while (current != null) {
            for (List<TID> tids : current.values) {
                result.addAll(tids);
            }
            current = current.next;
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IndexType getType() {
        return IndexType.BTREE;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    private LeafNode findLeaf(Comparable<?> key) {
        Node current = root;
        while (!current.isLeaf()) {
            InternalNode internal = (InternalNode) current;
            int idx = internal.childIndex(key);
            current = internal.children.get(idx);
        }
        return (LeafNode) current;
    }

    private void splitLeaf(LeafNode leaf) {
        int mid = leaf.keys.size() / 2;
        LeafNode sibling = new LeafNode();
        sibling.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
        sibling.values.addAll(leaf.values.subList(mid, leaf.values.size()));
        leaf.keys.subList(mid, leaf.keys.size()).clear();
        leaf.values.subList(mid, leaf.values.size()).clear();

        sibling.next = leaf.next;
        leaf.next = sibling;

        promote(leaf, sibling, sibling.keys.getFirst());
    }

    private void promote(Node left, Node right, Comparable<?> key) {
        if (left.parent == null) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(key);
            newRoot.children.add(left);
            newRoot.children.add(right);
            left.parent = newRoot;
            right.parent = newRoot;
            root = newRoot;
            height++;
            return;
        }
        InternalNode parent = left.parent;
        parent.insertChild(key, right);
        if (parent.isOverflow()) {
            splitInternal(parent);
        }
    }

    private void splitInternal(InternalNode node) {
        int midIndex = node.keys.size() / 2;
        Comparable<?> upKey = node.keys.get(midIndex);

        InternalNode sibling = new InternalNode();
        sibling.keys.addAll(node.keys.subList(midIndex + 1, node.keys.size()));
        sibling.children.addAll(node.children.subList(midIndex + 1, node.children.size()));

        for (Node child : sibling.children) {
            child.parent = sibling;
        }

        node.keys.subList(midIndex, node.keys.size()).clear();
        node.children.subList(midIndex + 1, node.children.size()).clear();

        promote(node, sibling, upKey);
    }

    private int compare(Comparable<?> a, Comparable<?> b) {
        @SuppressWarnings("unchecked")
        Comparable<Object> left = (Comparable<Object>) a;
        return left.compareTo(b);
    }

    private abstract class Node {
        protected final List<Comparable<?>> keys = new ArrayList<>();
        protected InternalNode parent;

        abstract boolean isLeaf();

    }

    private class InternalNode extends Node {
        private final List<Node> children = new ArrayList<>();

        @Override
        boolean isLeaf() {
            return false;
        }

        int childIndex(Comparable<?> key) {
            int idx = binarySearch(keys, key);
            if (idx >= 0) return idx + 1;
            return -idx - 1;
        }

        void insertChild(Comparable<?> key, Node child) {
            int idx = binarySearch(keys, key);
            if (idx >= 0) {
                keys.add(idx + 1, key);
                children.add(idx + 2, child);
            } else {
                int insertionPoint = -idx - 1;
                keys.add(insertionPoint, key);
                children.add(insertionPoint + 1, child);
            }
            child.parent = this;
        }

        private boolean isOverflow() {
            return keys.size() > 2 * order;
        }
    }

    private class LeafNode extends Node {
        private final List<List<TID>> values = new ArrayList<>();
        private LeafNode next;

        @Override
        boolean isLeaf() {
            return true;
        }

        void insert(Comparable<?> key, TID tid) {
            int idx = binarySearch(keys, key);
            if (idx >= 0) {
                values.get(idx).add(tid);
                return;
            }
            int insertionPoint = -idx - 1;
            keys.add(insertionPoint, key);
            List<TID> tids = new ArrayList<>();
            tids.add(tid);
            values.add(insertionPoint, tids);
        }

        private boolean isOverflow() {
            return keys.size() > 2 * order;
        }

        List<TID> get(Comparable<?> key) {
            int idx = binarySearch(keys, key);
            if (idx >= 0) {
                return values.get(idx);
            }
            return List.of();
        }

    }

    private int binarySearch(List<Comparable<?>> elements, Comparable<?> key) {
        int low = 0;
        int high = elements.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = compare(elements.get(mid), key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }
}
