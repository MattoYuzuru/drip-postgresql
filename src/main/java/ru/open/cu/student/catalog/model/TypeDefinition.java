package ru.open.cu.student.catalog.model;

import java.io.*;

public class TypeDefinition {
    /*
     * oid: Int — уникальный идентификатор типа.
     * name: String — имя типа (например, INT, VARCHAR_256).
     * byteLength: Int — размер в байтах или -1 для переменной длины.
     */
    public Integer oid;
    public String name;
    public Integer byteLength;

    public TypeDefinition(Integer oid, String name, Integer byteLength) {
        this.oid = oid;
        this.name = name;
        this.byteLength = byteLength;
    }

    public TypeDefinition(String name) {
        this.name = name;
    }

    public Integer getOid() {
        return oid;
    }

    public void setOid(Integer oid) {
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getByteLength() {
        return byteLength;
    }

    public void setByteLength(Integer byteLength) {
        this.byteLength = byteLength;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(oid != null ? oid : 0);
        dos.writeUTF(name != null ? name : "");
        dos.writeInt(byteLength != null ? byteLength : -1);

        return bos.toByteArray();
    }

    public static TypeDefinition fromBytes(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

        int oid = dis.readInt();
        String name = dis.readUTF();
        int byteLength = dis.readInt();

        return new TypeDefinition(oid, name, byteLength);
    }
}
