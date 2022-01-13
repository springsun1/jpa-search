package easyjpa.repository;

import com.dbapp.ejpa.repository.BaseRepository;
import easyjpa.model.auto.Account;
import easyjpa.model.auto.AccountDept;
import org.springframework.stereotype.Component;

/**
 * 课程持久层
 * @author dbdu
 */

@Component
public interface TestAccountRepository extends BaseRepository<AccountDept,Long> {

}
