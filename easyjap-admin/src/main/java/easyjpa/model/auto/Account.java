package easyjpa.model.auto;

import com.dbapp.ejpa.entity.BaseSearchEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * <p>
 * 
 * </p>
 *
 * @author dyt
 * @since 2022-01-13
 */
@Setter
@Getter
@Entity
@Table(name = "account")
public class Account  extends BaseSearchEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String name;

    private String account;

    private String password;

    private String descr;
    /**
     * 部门实体
     */
    @Column(name = "dept_id")
    private Long deptId;





}
