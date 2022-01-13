package easyjpa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@MapperScan("com.example.easyjpa.mapper")
@EnableJpaRepositories(basePackages = "easyjpa.repository")
public class EasyjpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyjpaApplication.class, args);
    }

}
