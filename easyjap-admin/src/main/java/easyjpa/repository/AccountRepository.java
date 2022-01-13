package easyjpa.repository;

import easyjpa.model.auto.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Component;

/**
 * 课程持久层
 * @author dbdu
 */

@Component
public interface AccountRepository extends JpaRepository<Account,Long>, JpaSpecificationExecutor<Account> {

}
