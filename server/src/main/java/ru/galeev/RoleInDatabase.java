package ru.galeev;

public class RoleInDatabase {
    private int id;
    private String name;

    public RoleInDatabase(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RoleInDatabase{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}