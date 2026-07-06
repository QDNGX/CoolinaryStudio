package org.example.projectcooking.dto.slot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Отмена слота с причиной (POST /slots/{id}/cancel, ADMIN — ФТ-04). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotCancelRequest {

    /** Обязательная причина отмены, ≤ 300 символов (Р-16); попадает в письмо клиентам (ФТ-15). */
    @NotBlank
    @Size(max = 300, message = "причина не длиннее 300 символов")
    private String reason;
}
