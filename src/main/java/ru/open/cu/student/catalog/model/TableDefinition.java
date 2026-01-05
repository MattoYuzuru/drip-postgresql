package ru.open.cu.student.catalog.model;

import java.io.*;

public class TableDefinition {

    /*
     * oid: Int — уникальный идентификатор таблицы.
     * name: String — имя таблицы.
     * type: String — тип объекта.
     * fileNode: String — имя файла с данными таблицы.
     * pagesCount: Int — количество страниц данных таблицы.
     */

    public Integer oid;
    public String name;
    public String type;
    public String fileNode;
    public Integer pagesCount;

    public TableDefinition(Integer oid, String name, String type, String fileNode, Integer pagesCount) {
        this.oid = oid;
        this.name = name;
        this.type = type;
        this.fileNode = fileNode;
        this.pagesCount = pagesCount;
    }

    public TableDefinition(String name) {
        this.name = name;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(oid != null ? oid : 0);
        dos.writeUTF(name != null ? name : "");
        dos.writeUTF(type != null ? type : "");
        dos.writeUTF(fileNode != null ? fileNode : "");
        dos.writeInt(pagesCount != null ? pagesCount : 0);

        return bos.toByteArray();
    }

    public static TableDefinition fromBytes(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

        int oid = dis.readInt();
        String name = dis.readUTF();
        String type = dis.readUTF();
        String fileNode = dis.readUTF();
        int pagesCount = dis.readInt();

        return new TableDefinition(oid, name, type, fileNode, pagesCount);
    }
}
