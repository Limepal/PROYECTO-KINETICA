package utec.kinetica.sign.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import utec.kinetica.sign.domain.Sign;


public interface SignRepository extends JpaRepository<Sign, Long>{
}
