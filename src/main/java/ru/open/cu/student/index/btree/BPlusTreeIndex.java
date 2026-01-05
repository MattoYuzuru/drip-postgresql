package ru.open.cu.student.index.btree;

import ru.open.cu.student.index.Index;
import ru.open.cu.student.index.TID;

import java.util.List;

public interface BPlusTreeIndex extends Index {

    List<TID> search(Comparable<?> key);

    List<TID> rangeSearch(Comparable<?> from, boolean includeFrom,
                          Comparable<?> to, boolean includeTo);

    List<TID> searchGreaterThan(Comparable<?> value, boolean inclusive);

    List<TID> searchLessThan(Comparable<?> value, boolean inclusive);

    List<TID> scanAll();
}
