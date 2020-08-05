package maskShop3;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminpageRepository extends CrudRepository<Adminpage, Long> {

    List<Adminpage> findByProductId(Long productId);

}