package br.com.estacionamento.exception;

import br.com.estacionamento.dto.ErroResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(TicketNaoEncontradoException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({EstacionamentoLotadoException.class, VeiculoJaEstacionadoException.class})
    public ResponseEntity<ErroResponse> conflito(RuntimeException e) {
        return erro(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErroResponse> concorrencia(ConcurrencyFailureException e) {
        return erro(HttpStatus.CONFLICT, "A vaga foi ocupada por outra requisição. Tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException e) {
        return erro(HttpStatus.CONFLICT, "Veículo já está no estacionamento");
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return erro(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(status.value(), mensagem));
    }
}
