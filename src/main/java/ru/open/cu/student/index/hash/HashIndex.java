package ru.open.cu.student.index.hash;

import ru.open.cu.student.index.Index;
import ru.open.cu.student.index.TID;

import java.util.List;

public interface HashIndex extends Index {

    List<TID> search(Comparable<?> key);


    long getRecordCount();

}
