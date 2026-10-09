package com.josenetoo_dev.veiculos_api;
import com.josenetoo_dev.veiculos_api.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
class JwtConfigurationTest {
 @Test void productionRejectsMissingSecret(){var env=new MockEnvironment();env.setActiveProfiles("prod");assertThrows(IllegalStateException.class,()->new JwtUtil("",1800000,env,null));}
 @Test void productionRejectsWeakAndKnownSecrets(){var env=new MockEnvironment();env.setActiveProfiles("prod");for(String s:new String[]{"short","AutoMinasSecretKeyMuitoLongaParaFuncionar2026Jose"})assertThrows(IllegalStateException.class,()->new JwtUtil(s,1800000,env,null));}
 @Test void prodCannotBeBypassedWithLocalProfile(){var env=new MockEnvironment();env.setActiveProfiles("local","prod");assertThrows(IllegalStateException.class,()->new JwtUtil("",1800000,env,null));}
 @Test void defaultProfileFailsClosed(){assertThrows(IllegalStateException.class,()->new JwtUtil("",1800000,new MockEnvironment(),null));}
 @Test void localCanUseEphemeralKey(){var env=new MockEnvironment();env.setActiveProfiles("local");assertDoesNotThrow(()->new JwtUtil("",1800000,env,null));}
 @Test void expiryCannotExceedThirtyMinutes(){assertThrows(IllegalStateException.class,()->new JwtUtil("test-only-key-at-least-thirty-two-bytes",86400000,new MockEnvironment(),null));}
}
