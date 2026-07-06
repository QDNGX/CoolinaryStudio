package org.example.projectcooking.dto.user;

import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

/**
 * Обновление своего профиля (PATCH /me). Клиент меняет только name (ФТ-21/Р-12) и
 * allergyNote (ФТ-18). Email и производные поля недоступны (Р-16). Для allergyNote явный
 * null очищает заметку; отсутствующее поле не меняет текущее значение.
 */
@NoArgsConstructor
public class UpdateMeRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String allergyNote;

    private boolean nameSet;
    private boolean allergyNoteSet;

    public UpdateMeRequest(String name, String allergyNote) {
        this.name = name;
        this.allergyNote = allergyNote;
        this.nameSet = true;
        this.allergyNoteSet = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.nameSet = true;
    }

    public String getAllergyNote() {
        return allergyNote;
    }

    public void setAllergyNote(String allergyNote) {
        this.allergyNote = allergyNote;
        this.allergyNoteSet = true;
    }

    public boolean isNameSet() {
        return nameSet;
    }

    public boolean isAllergyNoteSet() {
        return allergyNoteSet;
    }
}
