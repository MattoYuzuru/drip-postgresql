package ru.open.cu.student.catalog.model;

import java.io.*;

public class ColumnDefinition {
    /*
     * oid: Int — уникальный идентификатор колонки.
     * tableOid: Int — ссылка на таблицу (TableDefinition.oid).
     * typeOid: Int — ссылка на тип (TypeDefinition.oid).
     * name: String — имя колонки.
     * position: int — порядковый номер колонки (с 0).
     * */

    public Integer oid;
    public Integer tableOid;
    public Integer typeOid;
    public String name;
    public Integer position;

    public ColumnDefinition(Integer oid, Integer tableOid, Integer typeOid, String name, Integer position) {
        this.oid = oid;
        this.tableOid = tableOid;
        this.typeOid = typeOid;
        this.name = name;
        this.position = position;
    }

    public ColumnDefinition(String name) {
        this.name = name;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(oid != null ? oid : 0);
        dos.writeInt(tableOid != null ? tableOid : 0);
        dos.writeInt(typeOid != null ? typeOid : 0);
        dos.writeUTF(name != null ? name : "");
        dos.writeInt(position != null ? position : 0);

        return bos.toByteArray();
    }

    public static ColumnDefinition fromBytes(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

        int oid = dis.readInt();
        int tableOid = dis.readInt();
        int typeOid = dis.readInt();
        String name = dis.readUTF();
        int position = dis.readInt();

        return new ColumnDefinition(oid, tableOid, typeOid, name, position);
    }
}
