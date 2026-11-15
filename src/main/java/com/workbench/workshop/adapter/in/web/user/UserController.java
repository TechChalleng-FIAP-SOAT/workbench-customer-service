package com.workbench.workshop.adapter.in.web.user;

import com.workbench.workshop.adapter.in.web.user.UserDtos.AuthValidateRequest;
import com.workbench.workshop.adapter.in.web.user.UserDtos.AuthValidateResponse;
import com.workbench.workshop.adapter.in.web.user.UserDtos.SignUpRequest;
import com.workbench.workshop.adapter.in.web.user.UserDtos.SignUpResponse;
import com.workbench.workshop.application.user.AuthenticateUserUseCase;
import com.workbench.workshop.application.user.SignUpUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Identidade do staff (paridade com o UserController do monólito). O Customer
 * Service guarda os usuários; a emissão do JWT é da Lambda de auth, que chama
 * {@code POST /api/users/auth/validate} para conferir as credenciais.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "Cadastro e validação de usuários staff")
public class UserController {

    private final SignUpUseCase signUp;
    private final AuthenticateUserUseCase authenticateUser;

    @PostMapping("/signup")
    @Operation(summary = "Cadastrar usuário staff (senha BCrypt)")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        var result = signUp.execute(request.email(), request.password(), request.roles());
        log.info("Usuário staff cadastrado: id={}, email={}", result.id(), result.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignUpResponse(result.id(), result.email(), result.roles()));
    }

    @PostMapping("/auth/validate")
    @Operation(summary = "Validar credenciais de staff (uso interno da Lambda de auth)")
    public ResponseEntity<AuthValidateResponse> validate(@Valid @RequestBody AuthValidateRequest request) {
        var result = authenticateUser.execute(request.email(), request.password());
        return ResponseEntity.ok(new AuthValidateResponse(result.email(), result.roles()));
    }

    // --- DTOs ---
}
