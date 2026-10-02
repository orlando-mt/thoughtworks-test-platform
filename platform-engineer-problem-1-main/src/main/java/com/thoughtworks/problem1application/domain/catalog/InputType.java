package com.thoughtworks.problem1application.domain.catalog;

/** Tipos de Terraform que se le pueden preguntar a un usuario en un chat o formulario. */
public enum InputType {
    STRING("string", "string"),
    NUMBER("number", "number"),
    BOOLEAN("bool", "boolean");

    private final String terraformType;
    private final String jsonType;

    InputType(String terraformType, String jsonType) {
        this.terraformType = terraformType;
        this.jsonType = jsonType;
    }

    public String jsonType() {
        return jsonType;
    }

    /** null si el tipo es complejo (list, map, object): esos no se preguntan. */
    public static InputType fromTerraform(String type) {
        for (InputType value : values()) {
            if (value.terraformType.equals(type)) {
                return value;
            }
        }
        return null;
    }
}