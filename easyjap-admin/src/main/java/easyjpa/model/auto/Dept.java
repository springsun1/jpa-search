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
@Table(name = "dept")
public class Dept extends BaseSearchEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "dept_name")
    private String deptName;

    @Column(name = "dept_descr")
    private String deptDescr;



}
