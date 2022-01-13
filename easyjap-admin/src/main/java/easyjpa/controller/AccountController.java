package easyjpa.controller;

import com.dbapp.ejpa.BaseController;
import com.dbapp.ejpa.entity.BaseSearchEntity;
import easyjpa.model.auto.Account;
import easyjpa.model.auto.AccountDept;
import easyjpa.repository.AccountRepository;
import easyjpa.repository.TestAccountRepository;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import vip.efactory.common.base.entity.BaseSearchField;
import vip.efactory.common.base.utils.R;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author dyt
 * @since 2022-01-13
 */
@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController extends BaseController<AccountDept, TestAccountRepository, Long> {


    @Autowired
    private AccountRepository  accountRepository;

    @GetMapping("/getUser")
    public R getUser(@PageableDefault(page=0,size=4)Pageable pageable){

        Account account1 = new Account();
        account1.setName("");
        account1.setAccount("");
        account1.setPassword("");
        Set<BaseSearchField> searchFields = new HashSet<>();
        BaseSearchField baseSearchField = new BaseSearchField();
        baseSearchField.setName("name");
        baseSearchField.setSearchType(0);
        baseSearchField.setVal("test");
        searchFields.add(baseSearchField);

        account1.setConditions(searchFields);
        if (account1.getConditions() != null && account1.getConditions().size() > 0) {
            //构造动态查询的条件
//            R r = super.advancedQueryByPage(pageable, account1);
//            log.info("返回結果{}");
//            return r;
        }

        return null;
    }
    /**
     * Description: 高级查询
     *
     * @param baseSearchEntity 含有高级查询条件
     * @param page             分页参数对象
     * @return vip.efactory.ejpa.utils.R
     * @author dbdu
     */
    @ApiOperation(value = "多条件组合查询,返回分页数据", notes = "默认每页25条记录,id字段降序")
    @RequestMapping(value = "/advanced/query", method = {RequestMethod.POST})
    public R advancedQuery(@RequestBody BaseSearchEntity baseSearchEntity, @PageableDefault(value = 25, sort = {"id"}, direction = Sort.Direction.DESC) Pageable page) {
        AccountDept entity = new AccountDept();
//        Account entity = new Account();
        BeanUtils.copyProperties(baseSearchEntity, entity);
        return super.advancedQueryByPage(page, entity);
    }


}
