package ru.open.cu.student.index.hash;

import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.index.TID;

import java.util.*;

public class HashIndexImpl implements HashIndex {

    private static final int INITIAL_LEVEL = 4; // 2^4 buckets
    private static final int DEFAULT_SPLIT_THRESHOLD = 8;

    private final String name;
    private final String tableName;
    private final String columnName;
    private final int bucketSplitThreshold;

    private final List<List<BucketEntry>> buckets = new ArrayList<>();
    private int level = INITIAL_LEVEL;
    private int nextSplitPointer = 0;
    private long recordCount = 0;

    public HashIndexImpl(String name, String tableName, String columnName) {
        this(name, tableName, columnName, DEFAULT_SPLIT_THRESHOLD);
    }

    public HashIndexImpl(String name, String tableName, String columnName, int bucketSplitThreshold) {
        this.name = name;
        this.tableName = tableName;
        this.columnName = columnName;
        this.bucketSplitThreshold = bucketSplitThreshold;
        int initialBuckets = 1 << INITIAL_LEVEL;
        for (int i = 0; i < initialBuckets; i++) {
            buckets.add(new ArrayList<>());
        }
    }

    @Override
    public synchronized void insert(Comparable<?> key, TID tid) {
        if (key == null || tid == null) return;
        int hash = hash(key);
        int bucketId = computeBucket(hash);
        List<BucketEntry> bucket = ensureBucket(bucketId);
        bucket.add(new BucketEntry(hash, key, tid));
        recordCount++;

        if (bucket.size() > bucketSplitThreshold) {
            performSplit();
        }
    }

    @Override
    public synchronized List<TID> search(Comparable<?> key) {
        if (key == null) return List.of();
        int hash = hash(key);
        int bucketId = computeBucket(hash);
        List<BucketEntry> bucket = ensureBucket(bucketId);
        List<TID> result = new ArrayList<>();
        for (BucketEntry entry : bucket) {
            if (entry.hash == hash && Objects.equals(entry.key, key)) {
                result.add(entry.tid);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IndexType getType() {
        return IndexType.HASH;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }


    @Override
    public synchronized long getRecordCount() {
        return recordCount;
    }

    private int hash(Comparable<?> key) {
        return key.hashCode();
    }

    private List<BucketEntry> ensureBucket(int id) {
        while (id >= buckets.size()) {
            buckets.add(new ArrayList<>());
        }
        return buckets.get(id);
    }

    private int computeBucket(int hash) {
        int mask = (1 << level) - 1;
        int bucket = hash & mask;
        if (bucket < nextSplitPointer) {
            bucket = hash & ((1 << (level + 1)) - 1);
        }
        if (bucket < 0) bucket = -bucket;
        return bucket;
    }

    private void performSplit() {
        int bucketToSplit = nextSplitPointer;
        if (bucketToSplit >= buckets.size()) {
            bucketToSplit = 0;
            nextSplitPointer = 0;
        }
        List<BucketEntry> entries = new ArrayList<>(buckets.get(bucketToSplit));
        buckets.get(bucketToSplit).clear();

        buckets.add(new ArrayList<>());

        nextSplitPointer++;
        if (nextSplitPointer >= (1 << level)) {
            nextSplitPointer = 0;
            level++;
        }

        for (BucketEntry entry : entries) {
            int bucket = computeBucket(entry.hash);
            ensureBucket(bucket).add(entry);
        }
    }

    private record BucketEntry(int hash, Comparable<?> key, TID tid) {
    }
}
