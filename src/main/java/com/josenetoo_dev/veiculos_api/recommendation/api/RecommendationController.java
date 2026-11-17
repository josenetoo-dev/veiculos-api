package com.josenetoo_dev.veiculos_api.recommendation.api;

import com.josenetoo_dev.veiculos_api.recommendation.application.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/v1/recommendations")
public class RecommendationController {
    private final Signals signals;
    private final HomeService homes;
    private final Map<String,Integer> requests=new HashMap<>();
    private long minute;
    public RecommendationController(Signals signals,HomeService homes) {this.signals=signals;this.homes=homes;}
    private synchronized void limit(String key) {
        long current=System.currentTimeMillis()/60000;
        if (minute!=current) {requests.clear();minute=current;}
        if ((!requests.containsKey(key) && requests.size()>=20000) || requests.merge(key,1,Integer::sum)>60)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    }
    @PostMapping("/events")
    @Operation(summary="Registrar sinal consentido. Reenvios são idempotentes.")
    public ResponseEntity<Void> event(@RequestBody @Valid Signals.Event event, Principal principal,
            @CookieValue(name="am_rec",required=false) UUID cookie,HttpServletRequest request) {
        Long user=principal==null?null:Long.valueOf(principal.getName());
        limit(user==null?"ip/"+request.getRemoteAddr():"user/"+user);
        UUID session=cookie==null?UUID.randomUUID():cookie;
        signals.record(event,session.toString(),user);
        var response=ResponseEntity.noContent();
        if (cookie==null) response.header(HttpHeaders.SET_COOKIE,ResponseCookie.from("am_rec",session.toString())
                .httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/v1/recommendations")
                .maxAge(604800).build().toString());
        return response.build();
    }
    @GetMapping("/home")
    @Operation(summary="Home com filtros inclusivos de preço em reais e UF")
    public ResponseEntity<HomeService.Home> home(@RequestParam(required=false) BigDecimal minPrice,
            @RequestParam(required=false) BigDecimal maxPrice,@RequestParam(required=false) String uf,
            @CookieValue(name="am_rec",required=false) UUID cookie,Principal principal) {
        if ((minPrice!=null && minPrice.signum()<0) || (maxPrice!=null && maxPrice.signum()<0)
                || (minPrice!=null && maxPrice!=null && minPrice.compareTo(maxPrice)>0)
                || (uf!=null && !uf.matches("[A-Za-z]{2}"))) throw new IllegalArgumentException();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(homes.home(minPrice,maxPrice,
                uf==null?null:uf.toUpperCase(Locale.ROOT),cookie==null?null:cookie.toString(),
                principal==null?null:Long.valueOf(principal.getName())));
    }
    @DeleteMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Excluir todos os sinais associados ao titular do JWT")
    public void delete(Principal principal) {signals.delete(Long.parseLong(principal.getName()));}
}
