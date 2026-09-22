package com.testpulse.dto;

import com.testpulse.model.AccountDeletionRequest.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountDeletionStatusDto {

    @NotNull(message = "Status is required.")
    private Status status;
}
