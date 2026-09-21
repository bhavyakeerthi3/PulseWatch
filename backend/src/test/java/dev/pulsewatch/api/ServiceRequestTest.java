package dev.pulsewatch.api;
import jakarta.validation.*; import org.junit.jupiter.api.Test; import java.util.Set; import static org.junit.jupiter.api.Assertions.*;
class ServiceRequestTest {
 private final Validator validator=Validation.buildDefaultValidatorFactory().getValidator();
 @Test void validServiceRequestPassesValidation(){assertTrue(validator.validate(new ServiceRequest("payments","localhost",8081,"dev","/actuator/health",true)).isEmpty());}
 @Test void rejectsPortOutsideTcpRange(){Set<ConstraintViolation<ServiceRequest>> violations=validator.validate(new ServiceRequest("payments","localhost",70000,"dev","/health",true));assertTrue(violations.stream().anyMatch(v->v.getPropertyPath().toString().equals("port")));}
 @Test void healthEndpointMustBeAbsolutePath(){assertFalse(validator.validate(new ServiceRequest("payments","localhost",8081,"dev","health",true)).isEmpty());}
}
