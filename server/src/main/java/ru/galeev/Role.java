package ru.galeev;

public enum Role {
    ADMIN, USER;

    public static Role fromValue(String value) {
        for (Role role : Role.values()) {
            if (value.equalsIgnoreCase(role.name())) {
                return role;
            }
        }
        return null;
    }
}