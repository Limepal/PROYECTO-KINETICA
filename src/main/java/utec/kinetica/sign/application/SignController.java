package utec.kinetica.sign.application;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utec.kinetica.sign.domain.Sign;
import utec.kinetica.sign.domain.SignService;

import java.util.List;

@RestController
@RequestMapping("/signs")
@RequiredArgsConstructor
public class SignController {
    private final SignService service;

    @GetMapping
    ResponseEntity<List<Sign>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    ResponseEntity<Void> save(@RequestBody Sign order) {
        service.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
